package net.ssehub.teaching.exercise_submitter.server.rest.routes;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.ssehub.studentmgmt.backend_api.model.NotificationDto;
import net.ssehub.teaching.exercise_submitter.server.storage.EmptyStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtLoadingException;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;

public class NotificationRouteIT extends AbstractRestTest {

    @Test
    public void viewUpdated() {
        AtomicInteger numCalled = new AtomicInteger();
        setStuMgmtView(new StuMgmtView(null, null, null, null) {
            @Override
            public void fullReload() throws StuMgmtLoadingException {
            }
            @Override
            public void update(NotificationDto notification) throws StuMgmtLoadingException {
                numCalled.incrementAndGet();
            }
        });
        startServer();
        
        Response response = target.path("/notify")
                .request()
                .post(Entity.entity(new NotificationDto(), MediaType.APPLICATION_JSON));
        
        assertAll(
            () -> assertEquals(200, response.getStatus()),
            () -> assertEquals(1, numCalled.get()) // once called through /notify route
        );
    }
    
    @Test
    public void stuMgmtLoadingExceptionInternalServerError() {
        setStuMgmtView(new StuMgmtView(null, null, null, null) {
            @Override
            public void fullReload() throws StuMgmtLoadingException {
            }
            @Override
            public void update(NotificationDto notification) throws StuMgmtLoadingException {
                throw new StuMgmtLoadingException();
            }
        });
        startServer();
        
        Response response = target.path("/notify")
                .request()
                .post(Entity.entity(new NotificationDto(), MediaType.APPLICATION_JSON));
        
        assertAll(
            () -> assertEquals(500, response.getStatus()),
            () -> assertEquals("Error retrieving information from student management system",
                    response.getStatusInfo().getReasonPhrase())
        );
    }
    
    @Test
    public void storageUpdated() {
        AtomicInteger numCalled = new AtomicInteger();
        setStorage(new EmptyStorage() {
            @Override
            public void createOrUpdateAssignmentsFromView(StuMgmtView view) throws StorageException {
                numCalled.incrementAndGet();
            }
        });
        startServer();
        
        Response response = target.path("/notify")
                .request()
                .post(Entity.entity(new NotificationDto(), MediaType.APPLICATION_JSON));
        
        assertAll(
            () -> assertEquals(200, response.getStatus()),
            () -> assertEquals(1, numCalled.get())
        );
    }
    
    @Test
    public void storageExceptionInternalServerError() {
        setStorage(new EmptyStorage() {
            @Override
            public void createOrUpdateAssignmentsFromView(StuMgmtView view) throws StorageException {
                throw new StorageException("mock");
            }
        });
        startServer();
        
        Response response = target.path("/notify")
                .request()
                .post(Entity.entity(new NotificationDto(), MediaType.APPLICATION_JSON));
        
        assertAll(
            () -> assertEquals(500, response.getStatus()),
            () -> assertEquals("Unexpected storage exception: mock", response.getStatusInfo().getReasonPhrase())
        );
    }
    
}
