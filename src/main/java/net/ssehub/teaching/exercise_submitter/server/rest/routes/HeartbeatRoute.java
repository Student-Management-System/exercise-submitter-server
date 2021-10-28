package net.ssehub.teaching.exercise_submitter.server.rest.routes;

import java.util.logging.Logger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
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
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "status")
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
                content = {@Content(schema = @Schema(implementation = StatusDto.class))})
        }
    )
    @GET
    public Response heartbeat() {
        LOGGER.fine("Heartbeat request received");
        return Response
                .ok()
                .entity(StatusDto.OK)
                .build();
    }
    
    /**
     * Small wrapper class to return a status object.
     */
    public static class StatusDto {
        
        public static final StatusDto OK = new StatusDto().setStatus("ok");
        
        private String status;
       
        /**
         * Returns the status.
         * 
         * @return The status.
         */
        @Schema(example = "ok")
        public String getStatus() {
            return status;
        }
        
        /**
         * Sets the status.
         * 
         * @param status The status.
         * 
         * @return this.
         */
        public StatusDto setStatus(String status) {
            this.status = status;
            return this;
        }
        
    }
    
}
