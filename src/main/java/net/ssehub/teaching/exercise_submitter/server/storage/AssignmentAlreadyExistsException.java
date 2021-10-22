package net.ssehub.teaching.exercise_submitter.server.storage;

/**
 * An assignment with the given name already exists.
 * 
 * @author Adam
 */
public class AssignmentAlreadyExistsException extends StorageException {

    private static final long serialVersionUID = 7349488177038296515L;

    private String assignmentName;
    
    /**
     * Creates this exception.
     * 
     * @param assignmentName The name of the assignment that already exists.
     */
    public AssignmentAlreadyExistsException(String assignmentName) {
        super("An assignment with the name \"" + assignmentName + "\" already exists");
        this.assignmentName = assignmentName;
    }
    
    /**
     * Returns the name of the assignment that already exists.
     * 
     * @return The name of the assignment.
     */
    public String getAssignmentName() {
        return assignmentName;
    }
    
}
