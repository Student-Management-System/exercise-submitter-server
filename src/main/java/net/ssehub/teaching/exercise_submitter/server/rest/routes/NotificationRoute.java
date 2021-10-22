package net.ssehub.teaching.exercise_submitter.server.rest.routes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.ssehub.studentmgmt.backend_api.model.NotificationDto;

/**
 * The route for receiving notifications on assignment and group updates from the management system.
 * 
 * @author Adam
 */
@Path("/notify")
public class NotificationRoute {

    /**
     * Receiving point for notifications from the student management system.
     * 
     * @param notification The notification.
     */
    @Operation(
        description = "Receiver for notifications on assignment and groups from the student management system",
        tags = {"notification"}
    )
    @POST
    public void notification(
            @RequestBody NotificationDto notification) {
        
        System.out.println("Received notification: " + notification);
    }
    
}
