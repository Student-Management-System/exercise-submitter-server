package net.ssehub.teaching.exercise_submitter.server.rest.exceptions;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;

/**
 * Converts an {@link StorageException} into a HTTP response.
 * 
 * @author Adam
 */
@Provider
public class StorageExceptionMapper implements ExceptionMapper<StorageException> {

    private static final Logger LOGGER = Logger.getLogger(StorageExceptionMapper.class.getName());
    
    @Override
    public Response toResponse(StorageException exception) {
        LOGGER.log(Level.WARNING, "Unexpected storage exception", exception);
        
        return Response
                .status(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Unexpected storage exception: "
                        + exception.getMessage())
                .build();
    }

}
