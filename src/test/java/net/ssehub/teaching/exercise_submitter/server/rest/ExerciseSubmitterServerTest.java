package net.ssehub.teaching.exercise_submitter.server.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Path;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.server.auth.PermissiveAuthManager;
import net.ssehub.teaching.exercise_submitter.server.storage.EmptyStorage;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.EmptyStuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.submission.NoChecksSubmissionManager;
import net.ssehub.teaching.exercise_submitter.server.submission.SubmissionManager;

public class ExerciseSubmitterServerTest {

    @Test
    public void stopNotRunningThrows() {
        ExerciseSubmitterServer server = new ExerciseSubmitterServer();
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> server.stop());
        assertEquals("Server not running", e.getMessage());
    }
    
    @Test
    public void startWithNoPortThrows() {
        ExerciseSubmitterServer server = new ExerciseSubmitterServer();
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> server.start());
        assertEquals("Port not specified", e.getMessage());
    }
    
    @Test
    public void startWithNoSubmissionManagerThrows() {
        ExerciseSubmitterServer server = new ExerciseSubmitterServer();
        server.setPort(8000);
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> server.start());
        assertEquals("No SubmissionManager specified", e.getMessage());
    }
    
    @Test
    public void startWithNoStorageThrows() {
        ExerciseSubmitterServer server = new ExerciseSubmitterServer();
        server.setPort(8000);
        server.setSubmissionManager(new SubmissionManager(null, null));
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> server.start());
        assertEquals("No storage specified", e.getMessage());
    }
    
    @Test
    public void startWithNoAuthManagerThrows() {
        ExerciseSubmitterServer server = new ExerciseSubmitterServer();
        server.setPort(8000);
        server.setSubmissionManager(new NoChecksSubmissionManager(null));
        server.setStorage(new EmptyStorage());
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> server.start());
        assertEquals("No AuthManager specified", e.getMessage());
    }
    
    @Test
    public void startWithNoStuMgmtViewThrows() {
        ExerciseSubmitterServer server = new ExerciseSubmitterServer();
        server.setPort(8000);
        server.setSubmissionManager(new NoChecksSubmissionManager(null));
        server.setStorage(new EmptyStorage());
        server.setAuthManager(new PermissiveAuthManager());
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> server.start());
        assertEquals("No StuMgmtView specified", e.getMessage());
    }
    
    private ExerciseSubmitterServer createStartedServer() {
        ExerciseSubmitterServer server = new ExerciseSubmitterServer();
        try {
            Field serverField = server.getClass().getDeclaredField("server");
            serverField.setAccessible(true);
            serverField.set(server, GrizzlyHttpServerFactory.createHttpServer(URI.create("http://0.0.0.0:23456/"), false));
        } catch (ReflectiveOperationException e) {
            fail("Failed to set server field: " + e.getMessage());
        }
        
        return server;
    }
    
    @Test
    public void setPortStartedThrows() {
        ExerciseSubmitterServer server = createStartedServer();
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> server.setPort(2345));
        assertEquals("Server already started", e.getMessage());
    }
    
    @Test
    public void setSubmissionManagerStartedThrows() {
        ExerciseSubmitterServer server = createStartedServer();
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> server.setSubmissionManager(new NoChecksSubmissionManager(null)));
        assertEquals("Server already started", e.getMessage());
    }
    
    @Test
    public void setStorageStartedThrows() {
        ExerciseSubmitterServer server = createStartedServer();
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> server.setStorage(new EmptyStorage()));
        assertEquals("Server already started", e.getMessage());
    }
    
    @Test
    public void setAuthManagerStartedThrows() {
        ExerciseSubmitterServer server = createStartedServer();
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> server.setAuthManager(new PermissiveAuthManager()));
        assertEquals("Server already started", e.getMessage());
    }
    
    @Test
    public void setStuMgmtViewStartedThrows() {
        ExerciseSubmitterServer server = createStartedServer();
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> server.setStuMgmtView(new EmptyStuMgmtView()));
        assertEquals("Server already started", e.getMessage());
    }
    
    @Test
    public void setTlsKeystoreStartedThrows() {
        ExerciseSubmitterServer server = createStartedServer();
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> server.setTlsKeystore(Path.of("test"), "123456"));
        assertEquals("Server already started", e.getMessage());
    }
    
    @Test
    public void startAlreadyStartedThrows() {
        ExerciseSubmitterServer server = createStartedServer();
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> server.start());
        assertEquals("Server already started", e.getMessage());
    }
    
}
