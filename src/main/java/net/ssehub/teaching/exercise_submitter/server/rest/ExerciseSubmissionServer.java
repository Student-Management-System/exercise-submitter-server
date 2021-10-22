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
import net.ssehub.teaching.exercise_submitter.server.checks.Check;
import net.ssehub.teaching.exercise_submitter.server.checks.CheckstyleCheck;
import net.ssehub.teaching.exercise_submitter.server.checks.CliJavacCheck;
import net.ssehub.teaching.exercise_submitter.server.checks.EclipseConfigCheck;
import net.ssehub.teaching.exercise_submitter.server.checks.EncodingCheck;
import net.ssehub.teaching.exercise_submitter.server.checks.FileSizeCheck;
import net.ssehub.teaching.exercise_submitter.server.checks.InternalJavacCheck;
import net.ssehub.teaching.exercise_submitter.server.checks.JavacCheck;
import net.ssehub.teaching.exercise_submitter.server.rest.routes.HeartbeatRoute;
import net.ssehub.teaching.exercise_submitter.server.rest.routes.NotificationRoute;
import net.ssehub.teaching.exercise_submitter.server.rest.routes.SubmissionRoute;
import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.filesystem.FilesystemStorage;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.submission.SubmissionManager;

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
        FileSizeCheck fsCheck = new FileSizeCheck();
        fsCheck.setMaxFileSize(1024 * 1024); // 1 KiB
        fsCheck.setMaxSubmissionSize(1024 * 1024); // 1 KiB
        
        EncodingCheck encCheck = new EncodingCheck();
        
        EclipseConfigCheck eclCheckRejecting = new EclipseConfigCheck();
        
        EclipseConfigCheck eclCheckNonRejecting = new EclipseConfigCheck();
        eclCheckNonRejecting.setRequireJavaProject(true);
        eclCheckNonRejecting.setRequireCheckstyleProject(true);
        
        JavacCheck javacCheck = InternalJavacCheck.isSupported() ? new InternalJavacCheck() : new CliJavacCheck();
        javacCheck.setJavaVersion(11);
        
        CheckstyleCheck csCheck = new CheckstyleCheck(new File("checkstyle.xml"));
        
        submissionManager.addRejectingCheck(fsCheck);
        submissionManager.addRejectingCheck(encCheck);
        submissionManager.addRejectingCheck(eclCheckRejecting);
        
        submissionManager.addNonRejectingCheck(eclCheckNonRejecting);
        submissionManager.addNonRejectingCheck(javacCheck);
        submissionManager.addNonRejectingCheck(csCheck);
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
        
        ISubmissionStorage storage = new FilesystemStorage(Path.of(args[1]));
        SubmissionManager submissionManager = new SubmissionManager(storage);
        createChecks(submissionManager);
        
        StuMgmtView stuMgmtView = new StuMgmtView(createAuthenticatedMgmtApiClient(args[2], args[3], args[4], args[5]));
        AuthManager authManager = new AuthManager(args[2], stuMgmtView);
        
        HttpServer server = startServer("http://localhost:" + args[0] + "/",
                submissionManager, storage, authManager, stuMgmtView);
        
        try {
            stuMgmtView.update(null); // TODO: trigger an update after the notifications are listening
            storage.createOrUpdateAssignmentsFromView(stuMgmtView);
        } catch (StorageException | ApiException e) {
            e.printStackTrace();
        }
        
        System.out.println("Server listens at http://localhost:" + args[0] + "/");
        System.out.println("Press enter to stop the server");
        System.in.read();
        server.shutdown();
    }

}
