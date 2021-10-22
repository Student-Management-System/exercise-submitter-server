package net.ssehub.teaching.exercise_submitter.server.rest.exceptions;

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

    @Override
    public Response toResponse(NoSuchTargetException exception) {
        return Response
                .status(Status.NOT_FOUND.getStatusCode(), exception.getMessage())
                .build();
    }

}
