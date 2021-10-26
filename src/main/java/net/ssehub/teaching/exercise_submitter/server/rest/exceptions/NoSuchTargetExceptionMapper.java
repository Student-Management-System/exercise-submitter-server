package net.ssehub.teaching.exercise_submitter.server.rest.exceptions;

import java.util.logging.Logger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchTargetException;

/**
 * Converts an {@link NoSuchTargetException} into a HTTP response.
 * 
 * @author Adam
 */
@Provider
public class NoSuchTargetExceptionMapper implements ExceptionMapper<NoSuchTargetException> {

    private static final Logger LOGGER = Logger.getLogger(NoSuchTargetException.class.getName());
    
    @Override
    public Response toResponse(NoSuchTargetException exception) {
        LOGGER.info(() -> "Target not found: " + exception.getMessage());
        
        return Response
                .status(Status.NOT_FOUND.getStatusCode(), exception.getMessage())
                .build();
    }

}
