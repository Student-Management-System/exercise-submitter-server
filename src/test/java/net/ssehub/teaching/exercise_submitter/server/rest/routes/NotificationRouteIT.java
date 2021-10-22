package net.ssehub.teaching.exercise_submitter.server.rest.routes;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.ssehub.studentmgmt.backend_api.ApiException;
import net.ssehub.studentmgmt.backend_api.model.NotificationDto;
import net.ssehub.teaching.exercise_submitter.server.storage.EmptyStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;

public class NotificationRouteIT extends AbstractRestTest {

    @Test
    public void viewUpdated() throws ApiException {
        AtomicInteger numCalled = new AtomicInteger();
        setStuMgmtView(new StuMgmtView(null) {
            @Override
            protected void init() throws ApiException {
                numCalled.incrementAndGet();
            }
        });
        startServer();
        
        Response response = target.path("/notify")
                .request()
                .post(Entity.entity(new NotificationDto(), MediaType.APPLICATION_JSON));
        
        assertAll(
            () -> assertEquals(200, response.getStatus()),
            () -> assertEquals(2, numCalled.get()) // once in c'tor, once through /notify route
        );
    }
    
    @Test
    public void apiExcpetionInternalServerError() throws ApiException {
        AtomicInteger numCalled = new AtomicInteger();
        setStuMgmtView(new StuMgmtView(null) {
            @Override
            protected void init() throws ApiException {
                numCalled.incrementAndGet();
                if (numCalled.get() == 2) {
                    throw new ApiException();
                }
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
    public void storageUpdated() throws ApiException {
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
    public void storageExceptionInternalServerError() throws ApiException {
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
