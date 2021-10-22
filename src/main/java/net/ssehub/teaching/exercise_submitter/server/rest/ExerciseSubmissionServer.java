package net.ssehub.teaching.exercise_submitter.server.rest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import net.ssehub.teaching.exercise_submitter.server.rest.routes.HeartbeatRoute;
import net.ssehub.teaching.exercise_submitter.server.rest.routes.SubmissionRoute;
import net.ssehub.teaching.exercise_submitter.server.storage.filesystem.FilesystemStorage;
import net.ssehub.teaching.exercise_submitter.server.submission.SubmissionManager;

/**
 * Main class to start the rest server.
 * 
 * @author Adam
 */
public class ExerciseSubmissionServer {

    /**
     * Creates and starts the server.
     * 
     * @param baseUri The base URI to use.
     * @param submissionManager The submission manager to use.
     * 
     * @return The started server.
     */
    public static HttpServer startServer(String baseUri, SubmissionManager submissionManager) {
        ResourceConfig config = new ResourceConfig()
                .register(HeartbeatRoute.class)
                .register(new SubmissionRoute(submissionManager))
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
        SubmissionManager submissionManager = new SubmissionManager(new FilesystemStorage(Path.of("teststorage")));
        HttpServer server = startServer("http://localhost:4444/", submissionManager);
        System.out.println("Server listens at http://localhost:4444/");
        System.out.println("Press enter to stop the server");
        System.in.read();
        server.shutdown();
    }

}
