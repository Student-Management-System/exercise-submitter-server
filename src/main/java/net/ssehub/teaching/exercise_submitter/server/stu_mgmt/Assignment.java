package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.StateEnum;
import net.ssehub.studentmgmt.backend_api.model.ParticipantDto.RoleEnum;

/**
 * An assignment in the student management system.
 * 
 * @author Adam
 */
public class Assignment {

    private String name;
    
    private StateEnum state;
    
    private CollaborationEnum collaboration;
    
    private Map<String, Group> groupsByNames;

    /**
     * Creates an assignment.
     * 
     * @param name The name of this assignment.
     * @param state The state of this assignment.
     * @param collaboration The collaboration type of this assignment.
     */
    Assignment(String name, StateEnum state, CollaborationEnum collaboration) {
        this.name = name;
        this.state = state;
        this.collaboration = collaboration;
        this.groupsByNames = new HashMap<>();
    }
    
    /**
     * Returns the name of this assignment.
     * 
     * @return The name of this assignment.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the collaboration type of this assignment.
     * 
     * @return The collaboratio type of this assignment.
     */
    public CollaborationEnum getCollaboration() {
        return collaboration;
    }
    
    /**
     * Adds a group to this assignment.
     * 
     * @param group The group to add.
     */
    void addGroup(Group group) {
        this.groupsByNames.put(group.getName(), group); 
    }
    
    /**
     * Returns all groups in this assignment.
     * 
     * @return All groups as an unmodifiable collection.
     */
    public Collection<Group> getGroups() {
        return Collections.unmodifiableCollection(this.groupsByNames.values());
    }
    
    /**
     * Retrieves a group by name.
     * 
     * @param name The name of the group.
     * 
     * @return The group, or {@link Optional#empty()} if a group with this name does not exist.
     */
    public Optional<Group> getGroup(String name) {
        return Optional.ofNullable(groupsByNames.get(name));
    }
    
    /**
     * Returns the state of this assignment.
     * 
     * @return The state of this assignment.
     */
    StateEnum getState() {
        return state;
    }
    
    /**
     * Checks whether the given participant can submit to this assignment. This is based on the state of this assignment
     * and the role of the participant. 
     * 
     * @param participant The participant that should be checked.
     * 
     * @return Whether the given participant can submit to this assignment.
     */
    public boolean canSubmit(Participant participant) {
        boolean isTutor = participant.getRole() == RoleEnum.LECTURER || participant.getRole() == RoleEnum.TUTOR;
        boolean isStudent = participant.getRole() == RoleEnum.STUDENT;
        
        return isTutor || (this.state == StateEnum.IN_PROGRESS && isStudent);
    }
    
    /**
     * Checks whether the given participant can retrieve previous submission to this assignment. This is based on the
     * state of this assignment and the role of the participant. 
     * 
     * @param participant The participant that should be checked.
     * 
     * @return Whether the given participant can retrieve previous submissions to this assignment.
     */
    public boolean canReplay(Participant participant) {
        boolean isTutor = participant.getRole() == RoleEnum.LECTURER || participant.getRole() == RoleEnum.TUTOR;
        boolean isStudent = participant.getRole() == RoleEnum.STUDENT;
        
        boolean stateInProgressOrEvaluated = this.state == StateEnum.IN_PROGRESS || this.state == StateEnum.EVALUATED;
        
        return isTutor || (stateInProgressOrEvaluated && isStudent);
    }
    
}
