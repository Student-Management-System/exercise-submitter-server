package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

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

    /**
     * Creates an assignment.
     * 
     * @param name The name of this assignment.
     * @param state The state of this assignment.
     */
    Assignment(String name, StateEnum state) {
        this.name = name;
        this.state = state;
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
