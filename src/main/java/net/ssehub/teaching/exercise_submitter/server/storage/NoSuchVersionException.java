package net.ssehub.teaching.exercise_submitter.server.storage;

/**
 * The given version does not exist.
 * 
 * @author Adam
 */
public class NoSuchVersionException extends StorageException {

    private static final long serialVersionUID = -7922494911681552752L;

    private Version version;
    
    private String groupName;
    
    private String assignmentName;
    
    /**
     * Creates this exception.
     * 
     * @param assignmentName The name of the assignment that the version does not exist for.
     * @param groupName The name of the group that the version does not exist for.
     * @param version The version that does not exist.
     */
    public NoSuchVersionException(String assignmentName, String groupName, Version version) {
        super("The version \"" + version + "\" does not exist for assignment \"" + assignmentName + "\" and group \""
                + groupName + "\"");
        this.version = version;
        this.assignmentName = assignmentName;
        this.groupName = groupName;
    }
    
    /**
     * Returns the version that did not exist.
     * 
     * @return The version.
     */
    public Version getVersion() {
        return version;
    }
    
    /**
     * Returns the name of the assignment that the version does not exist for.
     * 
     * @return The assignment name;
     */
    public String getAssignmentName() {
        return assignmentName;
    }
    
    /**
     * Returns the name of the group that the version does not exist for.
     * 
     * @return The group name;
     */
    public String getGroupName() {
        return groupName;
    }
    
}
