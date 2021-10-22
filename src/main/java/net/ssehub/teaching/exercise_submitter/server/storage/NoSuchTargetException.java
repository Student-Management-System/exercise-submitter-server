package net.ssehub.teaching.exercise_submitter.server.storage;

/**
 * Indicates that the given submission target does not exist.
 * 
 * @author Adam
 */
public class NoSuchTargetException extends StorageException {

    private static final long serialVersionUID = -4647180918103452956L;
    
    /**
     * Creates this exception for a missing assignment.
     * 
     * @param course The course of the assignment that doesn't exist.
     * @param assignmentName The name of the assignment that doesn't exist.
     */
    public NoSuchTargetException(String course, String assignmentName) {
        super("The assignment " + assignmentName + " in course " + course + " does not exist");
    }
    
    /**
     * Creates this exception for a missing target.
     * 
     * @param target The assignment and group that doesn't exist.
     */
    public NoSuchTargetException(SubmissionTarget target) {
        super("The group " + target.getGroupName() + " for assignment " + target.getAssignmentName() + " in course "
                + target.getCourse() + " does not exist");
    }
    
    /**
     * Creates this exception for a missing version within the given target.
     * 
     * @param target The assignment and group.
     * @param version The version that does not exist in the target.
     */
    public NoSuchTargetException(SubmissionTarget target, Version version) {
        super("The version " + version.getUnixTimestamp() + "does not exist for group " + target.getGroupName()
                + " in assignment " + target.getAssignmentName() + " in course " + target.getCourse());
    }

}
