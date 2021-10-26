package net.ssehub.teaching.exercise_submitter.server.rest.routes;

import java.util.Map;
import java.util.logging.Logger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Simple route to check if the server is running.
 * 
 * @author Adam
 */
@Path("/heartbeat")
@Produces(MediaType.APPLICATION_JSON)
public class HeartbeatRoute {

    private static final Logger LOGGER = Logger.getLogger(HeartbeatRoute.class.getName());
    
    /**
     * Returns <code>{"status":"ok"}</code>.
     * 
     * @return A successful response.
     */
    @Operation(
        description = "Heartbeat to check if the server is running properly",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Server is running",
                content = {
                    @Content(examples = {
                        @ExampleObject(value = "{\"status\": \"ok\"}")
                    })
                })
        }
    )
    @GET
    public Response heartbeat() {
        LOGGER.fine("Heartbeat request received");
        return Response
                .ok()
                .entity(Map.of("status", "ok"))
                .build();
    }
    
}
