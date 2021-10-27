package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

import java.util.HashMap;
import java.util.Map;

/**
 * A group of participants that work together on an assignment.
 * 
 * @author Adam
 */
public class Group {

    private String mgmtId;
    
    private String name;
    
    private Map<String, Participant> participantsByName;

    /**
     * Creates a new group with the given name.
     * 
     * @param mgmtId The ID of this group in the student management system.
     * @param name The name of the group.
     */
    Group(String mgmtId, String name) {
        this.mgmtId = mgmtId;
        this.name = name;
        this.participantsByName = new HashMap<>();
    }
    
    /**
     * Returns the ID of this group in the student management system.
     * 
     * @return The ID.
     */
    String getMgmtId() {
        return mgmtId;
    }
    
    /**
     * Returns the name of this group.
     * 
     * @return The name of this group.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Adds a participant to this group.
     * 
     * @param participant The participant to add.
     */
    void addParticipant(Participant participant) {
        participantsByName.put(participant.getName(), participant);
    }
    
    /**
     * Checks if the given participant is in this group.
     * 
     * @param participant The participant to check.
     * 
     * @return Whether the participant is in this group.
     */
    public boolean hasParticipant(Participant participant) {
        return participantsByName.containsKey(participant.getName());
    }
    
}
