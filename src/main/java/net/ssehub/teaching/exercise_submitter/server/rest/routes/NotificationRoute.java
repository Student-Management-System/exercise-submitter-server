package net.ssehub.teaching.exercise_submitter.server.rest.routes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
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
    }

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
    public void notification(@RequestBody NotificationDto notification) {
        
        System.out.println("Received notification: " + notification);
        try {
            stuMgmtView.update(notification);
            storage.createOrUpdateAssignmentsFromView(stuMgmtView);
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (StorageException e) {
            e.printStackTrace(); // TODO: handle exceptions?
        }
    }
    
}
