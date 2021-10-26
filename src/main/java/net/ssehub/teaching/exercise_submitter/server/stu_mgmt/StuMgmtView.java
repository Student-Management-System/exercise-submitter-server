package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.ssehub.studentmgmt.backend_api.ApiClient;
import net.ssehub.studentmgmt.backend_api.ApiException;
import net.ssehub.studentmgmt.backend_api.api.AssignmentApi;
import net.ssehub.studentmgmt.backend_api.api.AssignmentRegistrationApi;
import net.ssehub.studentmgmt.backend_api.api.CourseApi;
import net.ssehub.studentmgmt.backend_api.api.CourseParticipantsApi;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.StateEnum;
import net.ssehub.studentmgmt.backend_api.model.CourseDto;
import net.ssehub.studentmgmt.backend_api.model.GroupDto;
import net.ssehub.studentmgmt.backend_api.model.NotificationDto;
import net.ssehub.studentmgmt.backend_api.model.ParticipantDto;
import net.ssehub.studentmgmt.backend_api.model.ParticipantDto.RoleEnum;
import net.ssehub.studentmgmt.sparkyservice_api.api.AuthControllerApi;
import net.ssehub.studentmgmt.sparkyservice_api.model.AuthenticationInfoDto;
import net.ssehub.studentmgmt.sparkyservice_api.model.CredentialsDto;
import net.ssehub.studentmgmt.sparkyservice_api.model.TokenDto;

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
     * @param username The name of the new participant.
     * @param role The role of the new participant.
     * 
     * @return The new participant.
     */
    protected Participant createParticipant(Course course, String username, RoleEnum role)  {
        Participant participant = new Participant(username, role);
        course.addParticipant(participant);
        return participant;
    }
    
    /**
     * Creates and adds an assignment to the given course.
     * <p>
     * Convenience method for test cases.
     * 
     * @param course The course to add the assignment to.
     * @param name The name of the assignment.
     * @param state The state of the assignment.
     * @param collaboration The collaboration type of this assignment.
     * 
     * @return The new assignment.
     */
    protected Assignment createAssignment(
            Course course, String name, StateEnum state, CollaborationEnum collaboration) {
        Assignment assignment = new Assignment(name, state, collaboration);
        course.addAssignment(assignment);
        return assignment;
    }
    
    /**
     * Creates an adds a group to the given assignment.
     * <p>
     * Convenience method for test cases.
     * 
     * @param assignment The assignment to add the group to.
     * @param name The name of the group.
     * @param participants The participants of this group.
     * 
     * @return The new group. 
     */
    protected Group createGroup(Assignment assignment, String name, Participant... participants) {
        Group group = new Group(name);
        assignment.addGroup(group);
        for (Participant participant : participants) {
            group.addParticipant(participant);
        }
        return group;
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
                
                createParticipant(course, pDto.getUsername(), pDto.getRole());
            }
            
            for (AssignmentDto aDto : assignmentApi.getAssignmentsOfCourse(course.getId())) {
                
                LOGGER.fine(() -> "Creating " + aDto.getCollaboration() + "-assignment " + aDto.getName()
                        + "(" + aDto.getStartDate() + ") in course " + courseId);
                
                Assignment assignment = createAssignment(
                        course, aDto.getName(), aDto.getState(), aDto.getCollaboration());
                
                for (GroupDto gDto : groupApi.getRegisteredGroups(course.getId(), aDto.getId(), null, null, null)) {
                    
                    LOGGER.fine(() -> "Creating group " + gDto.getName() + " with members "
                            + gDto.getMembers().stream()
                                .map(ParticipantDto::getUsername)
                                .collect(Collectors.joining(", "))
                            + " in assignment " + aDto.getName() + " in course " + courseId);
                    
                    createGroup(assignment, gDto.getName(), gDto.getMembers().stream()
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
    
}
