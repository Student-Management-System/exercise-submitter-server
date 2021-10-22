package net.ssehub.teaching.exercise_submitter.server.storage;

/**
 * An group with the given name does not exist.
 * 
 * @author Adam
 */
public class NoSuchGroupException extends StorageException {

    private static final long serialVersionUID = -4974740217711151976L;

    private String assignmentName;
    
    private String groupName;
    
    /**
     * Creates this exception.
     * 
     * @param assignmentName The name of the assignment that the group does not exist for.
     * @param groupName The name of the group that does not exist.
     */
    public NoSuchGroupException(String assignmentName, String groupName) {
        super("A group with the name \"" + groupName + "\" does not exist for assignment \"" + assignmentName + "\"");
        this.assignmentName = assignmentName;
        this.groupName = groupName;
    }

    /**
     * Returns the name of the assignment that the group does not exist for.
     * 
     * @return The assignment name;
     */
    public String getAssignmentName() {
        return assignmentName;
    }
    
    /**
     * Returns the name of the group that did not exist.
     * 
     * @return The name of the group.
     */
    public String getGroupName() {
        return groupName;
    }
    
}
