package net.ssehub.teaching.exercise_submitter.server.rest.routes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.net.ServerSocket;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import net.ssehub.teaching.exercise_submitter.server.auth.AuthManager;
import net.ssehub.teaching.exercise_submitter.server.auth.PermissiveAuthManager;
import net.ssehub.teaching.exercise_submitter.server.rest.ExerciseSubmissionServer;
import net.ssehub.teaching.exercise_submitter.server.storage.EmptyStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.EmptyStuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.submission.NoChecksSubmissionManager;
import net.ssehub.teaching.exercise_submitter.server.submission.SubmissionManager;

public abstract class AbstractRestTest {

    private HttpServer server;
    
    protected WebTarget target;
    
    private String uri;
    
    private SubmissionManager submissionManager;
    
    private ISubmissionStorage storage;
    
    private AuthManager authManager;
    
    private StuMgmtView stuMgmtView;
    
    @BeforeEach
    public void setupServer() {
        uri = "http://localhost:" + generateRandomPort() + "/";
        Client client = ClientBuilder.newClient();
        target = client.target(uri);
        
        storage = new EmptyStorage();
        submissionManager = new NoChecksSubmissionManager(storage);
        authManager = new PermissiveAuthManager();
        stuMgmtView = new EmptyStuMgmtView();
    }
    
    protected void setSubmissionManager(SubmissionManager submissionManager) {
        this.submissionManager = submissionManager;
    }
    
    protected void setStorage(ISubmissionStorage storage) {
        this.storage = storage;
    }
    
    protected void setAuthManager(AuthManager authManager) {
        this.authManager = authManager;
    }
    
    protected void setStuMgmtView(StuMgmtView stuMgmtView) {
        this.stuMgmtView = stuMgmtView;
    }
    
    protected void startServer() {
        assertDoesNotThrow(() -> stuMgmtView.fullReload());
        server = ExerciseSubmissionServer.startServer(uri, submissionManager, storage, authManager, stuMgmtView);
    }
    
    @AfterEach
    public void shutdownServer() {
        if (server != null) {
            server.shutdown();
        }
    }
    
    /**
     * Generates a random ephemeral port number.
     *  
     * @return A random open port.
     */
    public static int generateRandomPort() {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            port = socket.getLocalPort();
            
        } catch (IOException e) {
            System.err.println("Failed to get free port: " + e.getMessage());
            System.err.println("Using random port number (might not be free)");
            port = (int) (Math.random() * (65535 - 49152)) + 49152;
        }
        
        return port;
    }
    
}
