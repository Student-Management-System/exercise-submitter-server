package net.ssehub.teaching.exercise_submitter.server.rest.exceptions;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import net.ssehub.teaching.exercise_submitter.server.submission.UnauthorizedException;

/**
 * Converts an {@link UnauthorizedException} into a HTTP response.
 * 
 * @author Adam
 */
@Provider
public class UnauthorizedExceptionMapper implements ExceptionMapper<UnauthorizedException> {

    @Override
    public Response toResponse(UnauthorizedException exception) {
        return Response
                .status(Status.FORBIDDEN.getStatusCode(), "Unauthorized")
                .build();
    }

}
