package net.ssehub.teaching.exercise_submitter.server.rest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jsonb.JsonBindingFeature;
import org.glassfish.jersey.server.ResourceConfig;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.ssehub.teaching.exercise_submitter.server.auth.AuthManager;
import net.ssehub.teaching.exercise_submitter.server.logging.LoggingSetup;
import net.ssehub.teaching.exercise_submitter.server.rest.routes.HeartbeatRoute;
import net.ssehub.teaching.exercise_submitter.server.rest.routes.NotificationRoute;
import net.ssehub.teaching.exercise_submitter.server.rest.routes.SubmissionRoute;
import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.filesystem.FilesystemStorage;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtLoadingException;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.submission.SubmissionManager;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.Check;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.FileSizeCheck;

/**
 * Main class to start the rest server.
 * 
 * @author Adam
 */
@OpenAPIDefinition(
    info = @Info(
        title = "Exercise Submitter Server",
        description = "A sever for storing and retrieving exercise submissions.",
        version = "1"
    ),
    tags = {
        @Tag(name = "submission", description = "Sending and retrieving submission"),
        @Tag(name = "notification", description = "Notifcations on changes in the student management system"),
        @Tag(name = "status", description = "Server status")
    }
)
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class ExerciseSubmitterServer {

    private static final Logger LOGGER = Logger.getLogger(ExerciseSubmitterServer.class.getName());
    
    /**
     * Creates and starts the server.
     * 
     * @param baseUri The base URI to use.
     * @param submissionManager The {@link SubmissionManager} to use for new submissions.
     * @param storage The {@link ISubmissionStorage} to use for replaying old versions.
     * @param authManager The {@link AuthManager} to use for authentication and authorization.
     * @param stuMgmtView The view on the student management system.
     * 
     * @return The started server.
     */
    public static HttpServer startServer(String baseUri, SubmissionManager submissionManager,
            ISubmissionStorage storage, AuthManager authManager, StuMgmtView stuMgmtView) {
        
        ResourceConfig config = new ResourceConfig()
                .register(HeartbeatRoute.class)
                .register(new SubmissionRoute(submissionManager, storage, authManager))
                .register(new NotificationRoute(storage, stuMgmtView))
                .packages("net.ssehub.teaching.exercise_submitter.server.rest.exceptions")
                .register(JsonBindingFeature.class);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri), config);
    }
    
    /**
     * Creates the standard {@link Check}s for all courses. Currently this is only the {@link FileSizeCheck}.
     * 
     * @param submissionManager The manager to add the {@link Check}s to.
     */
    private static void createStandardChecks(SubmissionManager submissionManager) {
        FileSizeCheck fileSizeCheck = new FileSizeCheck();
        fileSizeCheck.setMaxFileSize(1024 * 1024); // 1 KiB
        fileSizeCheck.setMaxSubmissionSize(1024 * 1024); // 1 KiB
        
        submissionManager.addDefaultRejectingCheck(fileSizeCheck);
    }
    
    /**
     * Creates and starts the server with default configuration.
     *  
     * @param port The port to use.
     * @param storagePath The path to the {@link FilesystemStorage} to use.
     * @param authSystemUrl The URL to the authentication system (sparky-service) API.
     * @param stuMgmtUrl The URL to the student management system API.
     * @param username The username to authenticate this service as in the auth sytem.
     * @param password The password to authenticate this service with in the auth system.
     * 
     * @return The started HTTP server.
     * 
     * @throws IOException If creating the {@link FilesystemStorage} fails.
     */
    // checkstyle: stop parameter number check
    public static HttpServer startDefaultServer(int port, String storagePath, String authSystemUrl, String stuMgmtUrl,
            String username, String password) throws IOException {
    // checkstyle: resume parameter number check
        
        String url = "http://0.0.0.0:" + port + "/";
        
        LOGGER.config(() -> "Using storage directory " + storagePath);
        ISubmissionStorage storage = new FilesystemStorage(Path.of(storagePath));
        StuMgmtView stuMgmtView = new StuMgmtView(stuMgmtUrl, authSystemUrl, username, password);
        
        SubmissionManager submissionManager = new SubmissionManager(storage, stuMgmtView);
        createStandardChecks(submissionManager);
        
        AuthManager authManager = new AuthManager(authSystemUrl, stuMgmtView);

        LOGGER.config("Starting HTTP server on port " + port);
        HttpServer server = startServer(url, submissionManager, storage, authManager, stuMgmtView);
        
        boolean success = false;
        while (!success) {
            try {
                synchronized (SubmissionRoute.LOCK) {
                    stuMgmtView.fullReload();
                    storage.createOrUpdateAssignmentsFromView(stuMgmtView);
                }
                success = true;
            } catch (StorageException | StuMgmtLoadingException e) {
                LOGGER.log(Level.WARNING, "Failed to load intial student management system data; retrying...", e);
                
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                }
            }
        }
        
        return server;
        
    }
    
    /**
     * Calls {@link #startServer()}, waits for user input on {@link System#in} and then shuts down the server.
     * 
     * @param args Command line arguments. Content:
     *      <ol>
     *          <li>Port to listen on</li>
     *          <li>Path to storage directory</li>
     *          <li>URL to auth system (Sparky) API</li>
     *          <li>URL to student management system API</li>
     *      </ol>
     * 
     * @throws IOException If reading user input fails.
     */
    public static void main(String[] args) throws IOException {
        
        LoggingSetup.init();
        LoggingSetup.setLevel("INFO");
        LOGGER.info("Starting");
        
        HttpServer server = startDefaultServer(Integer.parseInt(args[0]), args[1], args[2], args[3], 
                System.getenv("SUBMISSION_SERVER_MGMT_USER"), System.getenv("SUBMISSION_SERVER_MGMT_PW"));
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down HTTP server");
            server.shutdown();
        }));
    }

}
