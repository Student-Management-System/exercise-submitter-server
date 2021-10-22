package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

/**
 * A local view of the courses, users, groups, and assignment in the student management system.
 *  
 * @author Adam
 */
public class StuMgmtView {

    private Map<String, Course> courses;
    
    private ApiClient mgmtClient;
    
    /**
     * Creates a view on the given student management system.
     * 
     * @param mgmtClient The client to query the student management system. Should already be authenticated via a token.
     * 
     * @throws ApiException If reading the initial data fails.
     */
    public StuMgmtView(ApiClient mgmtClient) throws ApiException {
        this.courses = new HashMap<>();
        this.mgmtClient = mgmtClient;
        init();
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
     * Initializes this view.
     */
    protected void init() throws ApiException {
        CourseApi courseApi = new CourseApi(mgmtClient);
        CourseParticipantsApi participantsApi = new CourseParticipantsApi(mgmtClient);
        AssignmentApi assignmentApi = new AssignmentApi(mgmtClient);
        AssignmentRegistrationApi groupApi = new AssignmentRegistrationApi(mgmtClient);
        
        for (CourseDto cDto : courseApi.getCourses(null, null, null, null, null)) {
            Course course = createCourse(cDto.getId());
            
            for (ParticipantDto pDto
                    : participantsApi.getUsersOfCourse(course.getId(), null, null, null, null, null)) {
                
                createParticipant(course, pDto.getUsername(), pDto.getRole());
            }
            
            for (AssignmentDto aDto : assignmentApi.getAssignmentsOfCourse(course.getId())) {
                Assignment assignment = createAssignment(
                        course, aDto.getName(), aDto.getState(), aDto.getCollaboration());
                
                for (GroupDto gDto : groupApi.getRegisteredGroups(course.getId(), aDto.getId(), null, null, null)) {
                    createGroup(assignment, gDto.getName(), gDto.getMembers().stream()
                            .map(ParticipantDto::getUsername)
                            .map(course::getParticipant)
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .toArray(s -> new Participant[s]));
                }
            }
        }
    }
    
    /**
     * Updates this view based on the given notification data.
     * 
     * @param notification The notification data.
     */
    public void update(NotificationDto notification) throws ApiException {
        // TODO: for now, just fully re-initialize
        courses.clear();
        init();
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
