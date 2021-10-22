package net.ssehub.teaching.exercise_submitter.server.rest.exceptions;

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

    @Override
    public Response toResponse(StorageException exception) {
        return Response
                .status(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Unexpected storage exception: "
                        + exception.getMessage())
                .build();
    }

}
