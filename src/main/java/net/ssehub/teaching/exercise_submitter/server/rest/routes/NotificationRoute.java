package net.ssehub.teaching.exercise_submitter.server.rest.routes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import net.ssehub.studentmgmt.backend_api.ApiException;
import net.ssehub.studentmgmt.backend_api.model.NotificationDto;
import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;

/**
 * The route for receiving notifications on assignment and group updates from the management system.
 * 
 * @author Adam
 */
@Path("/notify")
public class NotificationRoute {
    
    private ISubmissionStorage storage;
    
    private StuMgmtView stuMgmtView;
    
    /**
     * Creates the notification receiver.
     * 
     * @param storage The storage to create the assignment and group directories in.
     * @param stuMgmtView The view on the student management system that will be updated.
     */
    public NotificationRoute(ISubmissionStorage storage, StuMgmtView stuMgmtView) {
        this.storage = storage;
        this.stuMgmtView = stuMgmtView;
    }

    /**
     * Receiving point for notifications from the student management system.
     * 
     * @param notification The notification.
     * 
     * @return An HTTP response.
     * 
     * @throws StorageException If 
     */
    @Operation(
        description = "Receiver for notifications on assignment and groups from the student management system",
        responses = {
            @ApiResponse(responseCode = "200", description = "Notification received"),
            @ApiResponse(responseCode = "500", description = "An unexpected internal server error occurred")
        },
        tags = {"notification"}
    )
    @POST
    public Response notification(@RequestBody NotificationDto notification) throws StorageException {

        Response response;
        
        try {
            stuMgmtView.update(notification);
            storage.createOrUpdateAssignmentsFromView(stuMgmtView);
            
            response = Response.ok().build();
            
        } catch (ApiException e) {
            response = Response
                    .status(Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                            "Error retrieving information from student management system")
                    .build();
        }
        
        return response;
    }
    
}
