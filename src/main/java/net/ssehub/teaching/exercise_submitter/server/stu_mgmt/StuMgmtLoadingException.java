package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

/**
 * An exception thrown if loading data from the student management system fails.
 * 
 * @author Adam
 */
public class StuMgmtLoadingException extends Exception {

    private static final long serialVersionUID = 3219421234354813208L;

    /**
     * Creates an instance.
     */
    public StuMgmtLoadingException() {
    }
    
    /**
     * Creates an instance.
     * 
     * @param message A message describing the exception.
     * @param cause The cause of this exception.
     */
    public StuMgmtLoadingException(String message, Throwable cause) {
        super(message, cause);
    }

}
