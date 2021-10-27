package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

import net.ssehub.studentmgmt.backend_api.model.ParticipantDto.RoleEnum;

/**
 * A participant of a course in the student management system.
 * 
 * @author Adam
 */
public class Participant {

    private String mgmtId;
    
    private String name;
    
    private RoleEnum role;

    /**
     * Creates a new participant.
     * 
     * @param mgmtId The ID of the participant in the student management system.
     * @param name The name of the participant.
     * @param role The role of the participant.
     */
    Participant(String mgmtId, String name, RoleEnum role) {
        this.mgmtId = mgmtId;
        this.name = name;
        this.role = role;
    }
    
    /**
     * Returns the ID of the participant in the student management system.
     * 
     * @return The ID.
     */
    public String getMgmtId() {
        return mgmtId;
    }

    /**
     * Returns the username of this user.
     * 
     * @return The name of this user.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the role of this user in the course.
     * 
     * @return The role of the user.
     */
    public RoleEnum getRole() {
        return role;
    }
    
}
