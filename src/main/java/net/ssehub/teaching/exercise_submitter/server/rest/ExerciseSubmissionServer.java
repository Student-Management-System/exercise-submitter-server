package net.ssehub.teaching.exercise_submitter.server.rest;

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
import net.ssehub.teaching.exercise_submitter.server.auth.AuthManager;
import net.ssehub.teaching.exercise_submitter.server.rest.routes.HeartbeatRoute;
import net.ssehub.teaching.exercise_submitter.server.rest.routes.NotificationRoute;
import net.ssehub.teaching.exercise_submitter.server.rest.routes.SubmissionRoute;
import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.filesystem.FilesystemStorage;
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
     * 
     * @return The started server.
     */
    public static HttpServer startServer(String baseUri, SubmissionManager submissionManager,
            ISubmissionStorage storage, AuthManager authManager) {
        ResourceConfig config = new ResourceConfig()
                .register(HeartbeatRoute.class)
                .register(new SubmissionRoute(submissionManager, storage, authManager))
                .register(NotificationRoute.class)
                .packages("net.ssehub.teaching.exercise_submitter.server.rest.exceptions");
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(baseUri), config);
    }
    
    /**
     * Calls {@link #startServer()}, waits for user input on {@link System#in} and then shuts down the server.
     * 
     * @param args Ignored.
     * 
     * @throws IOException If reading user input fails.
     */
    public static void main(String[] args) throws IOException {
        ISubmissionStorage storage = new FilesystemStorage(Path.of("teststorage"));
        SubmissionManager submissionManager = new SubmissionManager(storage);
        AuthManager authManager = new AuthManager();
        
        HttpServer server = startServer("http://localhost:4444/", submissionManager, storage, authManager);
        System.out.println("Server listens at http://localhost:4444/");
        System.out.println("Press enter to stop the server");
        System.in.read();
        server.shutdown();
    }

}
