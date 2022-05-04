package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.ssehub.studentmgmt.backend_api.ApiClient;
import net.ssehub.studentmgmt.backend_api.ApiException;
import net.ssehub.studentmgmt.backend_api.api.AssessmentApi;
import net.ssehub.studentmgmt.backend_api.api.AssignmentApi;
import net.ssehub.studentmgmt.backend_api.api.AssignmentRegistrationApi;
import net.ssehub.studentmgmt.backend_api.api.CourseApi;
import net.ssehub.studentmgmt.backend_api.api.CourseParticipantsApi;
import net.ssehub.studentmgmt.backend_api.model.AssessmentCreateDto;
import net.ssehub.studentmgmt.backend_api.model.AssessmentDto;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.StateEnum;
import net.ssehub.studentmgmt.backend_api.model.CourseDto;
import net.ssehub.studentmgmt.backend_api.model.GroupDto;
import net.ssehub.studentmgmt.backend_api.model.MarkerDto;
import net.ssehub.studentmgmt.backend_api.model.MarkerDto.SeverityEnum;
import net.ssehub.studentmgmt.backend_api.model.NotificationDto;
import net.ssehub.studentmgmt.backend_api.model.PartialAssessmentDto;
import net.ssehub.studentmgmt.backend_api.model.ParticipantDto;
import net.ssehub.studentmgmt.backend_api.model.ParticipantDto.RoleEnum;
import net.ssehub.studentmgmt.backend_api.model.SubmissionConfigDto;
import net.ssehub.studentmgmt.sparkyservice_api.api.AuthControllerApi;
import net.ssehub.studentmgmt.sparkyservice_api.model.AuthenticationInfoDto;
import net.ssehub.studentmgmt.sparkyservice_api.model.CredentialsDto;
import net.ssehub.studentmgmt.sparkyservice_api.model.TokenDto;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.Check;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

/**
 * A local view of the courses, users, groups, and assignment in the student management system.
 *  
 * @author Adam
 */
public class StuMgmtView {

    private static final Logger LOGGER = Logger.getLogger(StuMgmtView.class.getName());
    
    private static final DateTimeFormatter TOKEN_EXPIRATION_PARSER = new DateTimeFormatterBuilder()
            .parseLenient()
            .parseCaseInsensitive()
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .appendLiteral('/')
            .appendValue(ChronoField.DAY_OF_MONTH, 2)
            .appendLiteral('/')
            .appendValue(ChronoField.YEAR, 2)
            .appendLiteral(' ')
            .appendValue(ChronoField.HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
            .toFormatter(Locale.ROOT);
    
    private static final String PARTIAL_ASSESSMENT_KEY = "exercise-submitter-checks";
    
    private static final String PARTIAL_ASSESSMENT_TITLE = "Automatic Checks";
    
    private static final String CHECK_CONFIGURATION_KEY = "exercise-submitter-checks";
    
    private Map<String, Course> courses;
    
    private ApiClient mgmtClient;
    
    private AuthControllerApi authApi;
    
    private String username;
    
    private String password;
    
    private Instant currentTokenExpiration;
    
    /**
     * Creates a view on the given student management system. The view is initially empty, call {@link #fullReload()} to
     * load initial data.
     * 
     * @param mgmtUrl The URL of the student management system.
     * @param authUrl The URL of the authentication system (sparky-service) to acquire tokens from.
     * @param username The username to log into the management system as.
     * @param password The password to log into the management system with.
     */
    public StuMgmtView(String mgmtUrl, String authUrl, String username, String password) {
        this.courses = new HashMap<>();

        this.mgmtClient = new ApiClient();
        this.mgmtClient.setBasePath(mgmtUrl);
        
        net.ssehub.studentmgmt.sparkyservice_api.ApiClient authClient
                = new net.ssehub.studentmgmt.sparkyservice_api.ApiClient();
        authClient.setBasePath(authUrl);
        this.authApi = new AuthControllerApi(authClient);
        
        this.username = username;
        this.password = password;
    }
    
    /**
     * Adds an authentication token retrieved from the authentication system to the {@link #mgmtClient}.
     * 
     * @throws StuMgmtLoadingException If authentication fails.
     */
    private void authenticateMgmtClient() throws StuMgmtLoadingException {
        if (currentTokenExpiration == null
                || currentTokenExpiration.isBefore(Instant.now().plus(1, ChronoUnit.HOURS))) {
            
            try {
                LOGGER.info(() -> "Logging into " + authApi.getApiClient().getBasePath()
                        + " with username " + username);
                
                AuthenticationInfoDto dto = authApi.authenticate(
                        new CredentialsDto().username(username).password(password));
                
                TokenDto token = dto.getToken();
                mgmtClient.setAccessToken(token.getToken());
                
                currentTokenExpiration = LocalDateTime.from(TOKEN_EXPIRATION_PARSER.parse(token.getExpiration()))
                        .toInstant(ZoneOffset.UTC);
                
            } catch (net.ssehub.studentmgmt.sparkyservice_api.ApiException e) {
                throw new StuMgmtLoadingException("Failed to authenticate as " + username, e);
                
            } catch (DateTimeParseException e) {
                LOGGER.log(Level.WARNING, "Can't parse token expiration: " + e.getParsedString(), e);
            }
        }
    }
    
    /**
     * Creates a course in this view.
     * <p>
     * Convenience method for test cases.
     * 
     * @param id The ID of he course.
     * 
     * @return The created course.
     */
    protected Course createCourse(String id) {
        Course course = new Course(id);
        courses.put(id, course);
        return course;
    }
    
    /**
     * Creates and adds a participant to the given course.
     * <p>
     * Convenience method for test cases.
     * 
     * @param course The course to add the participant to.
     * @param mgmtId The ID of the participant in the student management system.
     * @param username The name of the new participant.
     * @param role The role of the new participant.
     * 
     * @return The new participant.
     */
    protected Participant createParticipant(Course course, String mgmtId, String username, RoleEnum role)  {
        Participant participant = new Participant(mgmtId, username, role);
        course.addParticipant(participant);
        return participant;
    }
    
    /**
     * Creates and adds an assignment to the given course.
     * <p>
     * Convenience method for test cases.
     * 
     * @param course The course to add the assignment to.
     * @param mgmtId The ID of the assignment in the student management system.
     * @param name The name of the assignment.
     * @param state The state of the assignment.
     * @param collaboration The collaboration type of this assignment.
     * 
     * @return The new assignment.
     */
    protected Assignment createAssignment(
            Course course, String mgmtId, String name, StateEnum state, CollaborationEnum collaboration) {
        Assignment assignment = new Assignment(mgmtId, name, state, collaboration);
        course.addAssignment(assignment);
        return assignment;
    }
    
    /**
     * Creates an adds a group to the given assignment.
     * <p>
     * Convenience method for test cases.
     * 
     * @param assignment The assignment to add the group to.
     * @param mgmtId The ID of the group in the student management system.
     * @param name The name of the group.
     * @param participants The participants of this group.
     * 
     * @return The new group. 
     */
    protected Group createGroup(Assignment assignment, String mgmtId, String name, Participant... participants) {
        Group group = new Group(mgmtId, name);
        assignment.addGroup(group);
        for (Participant participant : participants) {
            group.addParticipant(participant);
        }
        return group;
    }
    
    /**
     * Retrieves the correct {@link SubmissionConfigDto} from the given {@link AssignmentDto} and sets its
     * configuration string for the given {@link Assignment}. If no such configuration string exists, nothing is set.
     * 
     * @param assignment The assignment to set the configuration string for.
     * @param dto The DTO to get the configuration string form.
     */
    private void setCheckConfigurationString(Assignment assignment, AssignmentDto dto) {
        List<SubmissionConfigDto> configDtos = dto.getConfigs();
        if (configDtos != null) {
            for (SubmissionConfigDto configDto : configDtos) {
                if (configDto.getTool().equals(CHECK_CONFIGURATION_KEY)) {
                    try {
                        assignment.setCheckConfigurationString(configDto.getConfig());
                    } catch (IllegalArgumentException e) {
                        LOGGER.log(Level.WARNING, "Invalid check configuration string for assignment "
                                + assignment.getName() + ": " + configDto.getConfig(), e);
                    }
                }
            }
        }
    }
    
    /**
     * Re-loads the given course. The course is removed and re-created with updated information.
     * 
     * @param courseId The ID of the course to re-load.
     */
    private void updateCourse(String courseId) {
        LOGGER.info(() -> "Re-loading course " + courseId);
        Course course = createCourse(courseId);
        
        CourseParticipantsApi participantsApi = new CourseParticipantsApi(mgmtClient);
        AssignmentApi assignmentApi = new AssignmentApi(mgmtClient);
        AssignmentRegistrationApi groupApi = new AssignmentRegistrationApi(mgmtClient);
        
        try {
            for (ParticipantDto pDto
                    : participantsApi.getUsersOfCourse(course.getId(), null, null, null, null, null)) {
                
                LOGGER.fine(() -> "Creating " + pDto.getRole() + " " + pDto.getUsername()
                        + " in course " + courseId);
                
                createParticipant(course, pDto.getUserId(), pDto.getUsername(), pDto.getRole());
            }
            
            for (AssignmentDto aDto : assignmentApi.getAssignmentsOfCourse(course.getId())) {
                
                LOGGER.fine(() -> "Creating " + aDto.getCollaboration() + "-assignment " + aDto.getName()
                        + "(" + aDto.getStartDate() + ") in course " + courseId);
                
                Assignment assignment = createAssignment(
                        course, aDto.getId(), aDto.getName(), aDto.getState(), aDto.getCollaboration());
                setCheckConfigurationString(assignment, aDto);
                
                for (GroupDto gDto : groupApi.getRegisteredGroups(course.getId(), aDto.getId(), null, null, null)) {
                    
                    LOGGER.fine(() -> "Creating group " + gDto.getName() + " with members "
                            + gDto.getMembers().stream()
                                .map(ParticipantDto::getUsername)
                                .collect(Collectors.joining(", "))
                            + " in assignment " + aDto.getName() + " in course " + courseId);
                    
                    createGroup(assignment, gDto.getId(), gDto.getName(), gDto.getMembers().stream()
                            .map(ParticipantDto::getUsername)
                            .map(course::getParticipant)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toArray(s -> new Participant[s]));
                }
            }
        
        } catch (ApiException e) {
            LOGGER.warning(() -> "Failed to update course " + courseId + "; is " + username
                    + " enrolled as lecturer?");
        }
        
    }
    
    /**
     * Completely reloads this view. Discards all current data and pulls everything from the management system again.
     * 
     * @throws StuMgmtLoadingException If loading data from the student management system fails.
     */
    public void fullReload() throws StuMgmtLoadingException {
        LOGGER.info(() -> "Completely re-loading information");
        courses.clear();
        
        authenticateMgmtClient();
        CourseApi courseApi = new CourseApi(mgmtClient);
        
        try {
            for (CourseDto cDto : courseApi.getCourses(null, null, null, null, null)) {
                updateCourse(cDto.getId());
            }
            
        } catch (ApiException e) {
            throw new StuMgmtLoadingException("Failed to retrieve course list", e);
        }
        
        LOGGER.info(() -> "Loaded " + courses.size() + " courses");
    }
    
    /**
     * Updates this view based on the given notification data.
     * 
     * @param notification The notification data.
     * 
     * @throws StuMgmtLoadingException If loading data from the student management system fails.
     */
    public void update(NotificationDto notification) throws StuMgmtLoadingException {
        authenticateMgmtClient();
        
        if (notification.getCourseId() != null) {
            updateCourse(notification.getCourseId());
        } else {
            fullReload();
        }
    }
    
    /**
     * Returns all courses in this view.
     * 
     * @return All courses as an unmodifiable collection.
     */
    public Collection<Course> getCourses() {
        return Collections.unmodifiableCollection(this.courses.values());
    }
    
    /**
     * Retrieves a course by its identifier.
     * 
     * @param course The identifier of the course.
     * 
     * @return The course, or {@link Optional#empty()} if no course with this identifier exists.
     */
    public Optional<Course> getCourse(String course) {
        return Optional.ofNullable(courses.get(course));
    }

    /**
     * Sends the given result messages of an accepted submission as a draft assessment to the student management system.
     * 
     * @param target The target that was submitted.
     * @param resultMessages The messages created by the {@link Check}s.
     */
    public void sendSubmissionResult(SubmissionTarget target, List<ResultMessage> resultMessages) {
        
        try {
            Course course = getCourse(target.getCourse()).orElseThrow();
            Assignment assginment = course.getAssignment(target.getAssignmentName()).orElseThrow();
            Optional<Group> group = assginment.getGroup(target.getGroupName());
            String userOrGroupId;
            boolean isGroup;
            if (group.isPresent()) {
                userOrGroupId = group.get().getMgmtId();
                isGroup = true;
            } else {
                userOrGroupId = course.getParticipant(target.getGroupName()).orElseThrow().getMgmtId();
                isGroup = false;
            }
            
            authenticateMgmtClient();
            AssessmentApi api = new AssessmentApi(mgmtClient);
            
            AssessmentDto existingAssessment;
            if (isGroup) {
                existingAssessment = api.getAssessmentsForAssignment(
                        course.getId(), assginment.getMgmtId(), null, null, null, userOrGroupId, null, null, null)
                        .stream().findAny().orElse(null);
            } else {
                existingAssessment = api.getAssessmentsForAssignment(
                        course.getId(), assginment.getMgmtId(), null, null, null, null, userOrGroupId, null, null)
                        .stream().findAny().orElse(null);
            }
            
            if (existingAssessment == null) {
                AssessmentCreateDto newAssessment = new AssessmentCreateDto();
                if (isGroup) {
                    newAssessment.setGroupId(userOrGroupId);
                } else {
                    newAssessment.setUserId(userOrGroupId);
                }
                newAssessment.setAssignmentId(assginment.getMgmtId());
                newAssessment.setIsDraft(true);
                
                existingAssessment = api.createAssessment(newAssessment, course.getId(), assginment.getMgmtId());
            }
            
            if (existingAssessment.isIsDraft()) {
                api.setPartialAssessment(createPartialAssessment(resultMessages), course.getId(),
                        assginment.getMgmtId(), existingAssessment.getId());
                
            } else {
                LOGGER.warning(() -> "Existing assessment for " + target
                        + " is not a draft; not adding partial assessment");
            }
            
        } catch (NoSuchElementException e) {
            LOGGER.warning(() ->
                    "Failed to send submission result to management system: can't find assginment " + target);
            
        } catch (StuMgmtLoadingException | ApiException e) {
            LOGGER.log(Level.WARNING, "Failed to send submission result to management system", e);
        }
    }

    /**
     * Creates a {@link PartialAssessmentDto} for the given result messages.
     * 
     * @param resultMessages The messages to convert into markers in the partial assessment.
     * 
     * @return A {@link PartialAssessmentDto} with the given result message.
     */
    private static PartialAssessmentDto createPartialAssessment(List<ResultMessage> resultMessages) {
        PartialAssessmentDto partialAssessment = new PartialAssessmentDto();
        partialAssessment.setKey(PARTIAL_ASSESSMENT_KEY);
        partialAssessment.setTitle(PARTIAL_ASSESSMENT_TITLE);
        if (resultMessages.isEmpty()) {
            partialAssessment.setComment("No errors or warnings.");
        } else {
            partialAssessment.setComment("Found errors and/or warnings.");
        }
        
        partialAssessment.setMarkers(resultMessages.stream()
                .map(StuMgmtView::resultMessageToMarkerDto)
                .collect(Collectors.toList()));
        
        return partialAssessment;
    }

    /**
     * Converts a {@link ResultMessage} to a {@link MarkerDto}.
     * 
     * @param message The result message to convert.
     * 
     * @return The corresponding marker dto.
     */
    private static MarkerDto resultMessageToMarkerDto(ResultMessage message) {
        MarkerDto marker = new MarkerDto();
        
        marker.setComment("(" + message.getCheckName() + ") " + message.getMessage());
        
        marker.setSeverity(message.getType() == MessageType.WARNING
                ? SeverityEnum.WARNING : SeverityEnum.ERROR);
        
        if (message.getFile() != null) {
            marker.setPath(message.getFile().toString().replace('\\', '/'));
            
            if (message.getLine() != null) {
                marker.setStartLineNumber(BigDecimal.valueOf(message.getLine()));
                marker.setEndLineNumber(BigDecimal.valueOf(message.getLine()));
                
                if (message.getColumn() != null) {
                    marker.setStartColumn(BigDecimal.valueOf(message.getColumn()));
                    marker.setEndColumn(BigDecimal.valueOf(message.getColumn()));
                }
            }
        }
        return marker;
    }
    
}
