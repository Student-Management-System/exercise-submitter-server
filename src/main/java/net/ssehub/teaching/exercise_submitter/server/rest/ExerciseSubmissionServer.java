package net.ssehub.teaching.exercise_submitter.server.rest;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import net.ssehub.studentmgmt.backend_api.ApiClient;
import net.ssehub.studentmgmt.backend_api.ApiException;
import net.ssehub.studentmgmt.sparkyservice_api.api.AuthControllerApi;
import net.ssehub.studentmgmt.sparkyservice_api.model.CredentialsDto;
import net.ssehub.teaching.exercise_submitter.server.auth.AuthManager;
import net.ssehub.teaching.exercise_submitter.server.rest.routes.HeartbeatRoute;
import net.ssehub.teaching.exercise_submitter.server.rest.routes.NotificationRoute;
import net.ssehub.teaching.exercise_submitter.server.rest.routes.SubmissionRoute;
import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.filesystem.FilesystemStorage;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.submission.SubmissionManager;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.Check;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.CheckstyleCheck;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.CliJavacCheck;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.EncodingCheck;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.FileSizeCheck;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.InternalJavacCheck;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.JavacCheck;

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
    )
)
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class ExerciseSubmissionServer {

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
                .packages("net.ssehub.teaching.exercise_submitter.server.rest.exceptions");
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri), config);
    }
    
    /**
     * Creates an authenticated {@link ApiClient} for the student management system.
     * 
     * @param authUrl The URL to the auth system (sparky-service).
     * @param mgmtUrl The URL to the student management system.
     * @param user The username to authenticate as.
     * @param password The password to authenticate with.
     * 
     * @return An authenticated {@link ApiClient}.
     * 
     * @throws net.ssehub.studentmgmt.sparkyservice_api.ApiException If authentication fails.
     */
    private static ApiClient createAuthenticatedMgmtApiClient(String authUrl, String mgmtUrl,
            String user, String password)
            throws net.ssehub.studentmgmt.sparkyservice_api.ApiException {
        ApiClient mgmtClient = new ApiClient();
        mgmtClient.setBasePath(mgmtUrl);
        
        net.ssehub.studentmgmt.sparkyservice_api.ApiClient authClient
                = new net.ssehub.studentmgmt.sparkyservice_api.ApiClient();
        authClient.setBasePath(authUrl);
        
        CredentialsDto credentials = new CredentialsDto().username(user).password(password);
        
        String token = new AuthControllerApi(authClient).authenticate(credentials).getToken().getToken();
        
        mgmtClient.setAccessToken(token);
        return mgmtClient;
    }
    
    /**
     * Creates the standard {@link Check}s. TODO: make this configurable.
     * 
     * @param submissionManager The manager to add the {@link Check}s to.
     */
    private static void createChecks(SubmissionManager submissionManager) {
        FileSizeCheck fileSizeCheck = new FileSizeCheck();
        fileSizeCheck.setMaxFileSize(1024 * 1024); // 1 KiB
        fileSizeCheck.setMaxSubmissionSize(1024 * 1024); // 1 KiB
        
        EncodingCheck encodingCheck = new EncodingCheck();
        
        JavacCheck javacCheck = InternalJavacCheck.isSupported() ? new InternalJavacCheck() : new CliJavacCheck();
        javacCheck.setJavaVersion(11);
        
        CheckstyleCheck checkstyleCheck = new CheckstyleCheck(new File("checkstyle.xml"));
        
        submissionManager.addRejectingCheck(fileSizeCheck);
        submissionManager.addRejectingCheck(encodingCheck);
        
        submissionManager.addNonRejectingCheck(javacCheck);
        submissionManager.addNonRejectingCheck(checkstyleCheck);
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
     * @throws ApiException If creating the initial {@link StuMgmtView} fails.
     * @throws net.ssehub.studentmgmt.sparkyservice_api.ApiException If authenticating with the given username and
     *      password fails.
     */
    // checkstyle: stop parameter number check
    public static HttpServer startDefaultServer(int port, String storagePath, String authSystemUrl, String stuMgmtUrl,
            String username, String password)
            throws IOException, ApiException, net.ssehub.studentmgmt.sparkyservice_api.ApiException {
    // checkstyle: resume parameter number check
        
        String url = "http://localhost:" + port + "/";
        
        ISubmissionStorage storage = new FilesystemStorage(Path.of(storagePath));
        SubmissionManager submissionManager = new SubmissionManager(storage);
        createChecks(submissionManager);
        
        System.out.println("Logging into stu-mgmt system as " + username);
        StuMgmtView stuMgmtView = new StuMgmtView(
                createAuthenticatedMgmtApiClient(authSystemUrl, stuMgmtUrl, username, password));
        AuthManager authManager = new AuthManager(authSystemUrl, stuMgmtView);
        
        HttpServer server = startServer(url, submissionManager, storage, authManager, stuMgmtView);
        
        try {
            stuMgmtView.update(null); // TODO: trigger an update after the notifications are listening
            storage.createOrUpdateAssignmentsFromView(stuMgmtView);
        } catch (StorageException | ApiException e) {
            e.printStackTrace();
        }
        
        System.out.println("Server listens at " + url);
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
     *          <li>The username to authenticate as</li>
     *          <li>The password to authenticate with</li>
     *      </ol>
     * 
     * @throws IOException If reading user input fails.
     * @throws ApiException If reading from the student management system fails.
     * @throws net.ssehub.studentmgmt.sparkyservice_api.ApiException If authenticating fails.
     */
    public static void main(String[] args)
            throws IOException, ApiException, net.ssehub.studentmgmt.sparkyservice_api.ApiException {
        
        HttpServer server = startDefaultServer(Integer.parseInt(args[0]), args[1], args[2], args[3], 
                System.getenv("SUBMISSION_SERVER_MGMT_USER"), System.getenv("SUBMISSION_SERVER_MGMT_PW"));
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
        }));
    }

}
