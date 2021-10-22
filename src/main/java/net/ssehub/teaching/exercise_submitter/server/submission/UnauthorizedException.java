package net.ssehub.teaching.exercise_submitter.server.submission;

/**
 * Indicates that the user is not authorized to perform the operation.
 * 
 * @author Adam
 */
public class UnauthorizedException extends Exception {

    private static final long serialVersionUID = 5183877547987814320L;

    /**
     * Creates this excpetion.
     */
    public UnauthorizedException() {
        super();
    }

    /**
     * Creates this exception.
     * 
     * @param cause The cause of this exception.
     */
    public UnauthorizedException(Throwable cause) {
        super(cause);
    }

}
