package net.ssehub.teaching.exercise_submitter.server.storage;

/**
 * A generic exception caused by the storage.
 * 
 * @author Adam
 */
public class StorageException extends Exception {

    private static final long serialVersionUID = -8029745708734384975L;

    /**
     * Creates this exception.
     */
    public StorageException() {
        super();
    }
    
    /**
     * Creates this exception.
     * 
     * @param cause The cause.
     */
    public StorageException(Throwable cause) {
        super(cause);
    }
    
    /**
     * Creates this exception.
     * 
     * @param messsage The detail message.
     */
    public StorageException(String messsage) {
        super(messsage);
    }
    
    /**
     * Creates this exception.
     * 
     * @param messsage The detail message.
     * @param cause The cause.
     */
    public StorageException(String messsage, Throwable cause) {
        super(messsage, cause);
    }
    
}
