package net.ssehub.teaching.exercise_submitter.server.storage;

/**
 * An assignment with the given name does not exist.
 * 
 * @author Adam
 */
public class NoSuchAssignmentException extends StorageException {

    private static final long serialVersionUID = 7349488177038296515L;

    private String assignmentName;
    
    /**
     * Creates this exception.
     * 
     * @param assignmentName The name of the assignment that does not exist.
     */
    public NoSuchAssignmentException(String assignmentName) {
        super("An assignment with the name \"" + assignmentName + "\" does not exist");
        this.assignmentName = assignmentName;
    }
    
    /**
     * Returns the name of the assignment that did not exist.
     * 
     * @return The name of the assignment.
     */
    public String getAssignmentName() {
        return assignmentName;
    }
    
}
