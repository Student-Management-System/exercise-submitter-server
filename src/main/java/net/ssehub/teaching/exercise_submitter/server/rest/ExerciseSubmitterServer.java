package net.ssehub.teaching.exercise_submitter.server.rest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
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
import net.ssehub.teaching.exercise_submitter.server.rest.exceptions.NoSuchTargetExceptionMapper;
import net.ssehub.teaching.exercise_submitter.server.rest.exceptions.StorageExceptionMapper;
import net.ssehub.teaching.exercise_submitter.server.rest.exceptions.UnauthorizedExceptionMapper;
import net.ssehub.teaching.exercise_submitter.server.rest.filters.CorsFilter;
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
        version = RestApiVersion.VERSION
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
    
    private HttpServer server;
    
    private int port = -1;
    
    private SubmissionManager submissionManager;
    
    private ISubmissionStorage storage;
    
    private AuthManager authManager;
    
    private StuMgmtView stuMgmtView;
    
    private Path tlsKeystore;

    private String tlsKeystorePassword;
    
    /**
     * Sets the port that the server should use.
     * 
     * @param port The port to use.
     * 
     * @return this.
     * 
     * @throws IllegalStateException If the server is already started.
     */
    public ExerciseSubmitterServer setPort(int port) throws IllegalStateException {
        requireNotStarted();
        this.port = port;
        return this;
    }
    
    /**
     * Sets the {@link SubmissionManager} to use.
     * 
     * @param submissionManager The {@link SubmissionManager} to use.
     * 
     * @return this.
     * 
     * @throws IllegalStateException If the server is already started.
     */
    public ExerciseSubmitterServer setSubmissionManager(SubmissionManager submissionManager)
            throws IllegalStateException {
        requireNotStarted();
        this.submissionManager = submissionManager;
        return this;
    }
    
    /**
     * Sets the {@link ISubmissionStorage} to use.
     * 
     * @param storage The {@link ISubmissionStorage} to use.
     * 
     * @return this.
     * 
     * @throws IllegalStateException If the server is already started.
     */
    public ExerciseSubmitterServer setStorage(ISubmissionStorage storage) throws IllegalStateException {
        requireNotStarted();
        this.storage = storage;
        return this;
    }
    
    /**
     * Sets the {@link AuthManager} to use.
     * 
     * @param authManager The {@link AuthManager} to use.
     * 
     * @return this.
     * 
     * @throws IllegalStateException If the server is already started.
     */
    public ExerciseSubmitterServer setAuthManager(AuthManager authManager) throws IllegalStateException {
        requireNotStarted();
        this.authManager = authManager;
        return this;
    }
    
    /**
     * Sets the {@link StuMgmtView} to use.
     * 
     * @param stuMgmtView The {@link StuMgmtView} to use.
     * 
     * @return this.
     * 
     * @throws IllegalStateException If the server is already started.
     */
    public ExerciseSubmitterServer setStuMgmtView(StuMgmtView stuMgmtView) throws IllegalStateException {
        requireNotStarted();
        this.stuMgmtView = stuMgmtView;
        return this;
    }
    
    /**
     * Sets the path to the file containing the TLS server keypair to use. If this set, the server will serve HTTPS,
     * otherwise plain HTTP is used.
     * 
     * @param tlsKeystore The path to the keystore file.
     * @param password The password for this keystore file.
     * 
     * @return this.
     * 
     * @throws IllegalStateException If the server is already started.
     */
    public ExerciseSubmitterServer setTlsKeystore(Path tlsKeystore, String password) throws IllegalStateException {
        requireNotStarted();
        this.tlsKeystore = tlsKeystore;
        this.tlsKeystorePassword = password;
        return this;
    }
    
    /**
     * Starts the HTTP server.
     * 
     * @throws IllegalStateException If the server is already started or a required property was not set.
     */
    public void start() throws IllegalStateException {
        requireNotStarted();
        
        if (port <= 0) {
            throw new IllegalStateException("Port not specified");
        }
        
        ResourceConfig config = createConfig();
        
        if (tlsKeystore != null) {
            // HTTPS
            SSLContextConfigurator sslContextConfig = new SSLContextConfigurator();
            sslContextConfig.setKeyStoreFile(tlsKeystore.toAbsolutePath().toString());
            sslContextConfig.setKeyStorePass(tlsKeystorePassword);
            
            SSLEngineConfigurator sslEngineConfig = new SSLEngineConfigurator(sslContextConfig, false, false, false);
            
            server = GrizzlyHttpServerFactory.createHttpServer(
                    URI.create("https://0.0.0.0:" + port + "/"), config, true, sslEngineConfig);
            
        } else {
            // HTTP
            server = GrizzlyHttpServerFactory.createHttpServer(
                    URI.create("http://0.0.0.0:" + port + "/"), config);
        }
    }
    
    /**
     * Stops the HTTP server.
     * 
     * @throws IllegalStateException If the server is not running.
     */
    public void stop() throws IllegalStateException {
        if (server == null) {
            throw new IllegalStateException("Server not running");
        }
        server.shutdown();
        server = null;
    }
    
    /**
     * Ensures that the server is currently not running.
     * 
     * @throws IllegalStateException If the server is running.
     */
    private void requireNotStarted() throws IllegalStateException {
        if (server != null) {
            throw new IllegalStateException("Server already started");
        }
    }
    
    /**
     * Creates the {@link ResourceConfig} with all necessary registrations to serve our rest routes.
     * 
     * @return A {@link ResourceConfig} for our rest routes.
     * 
     * @throws IllegalStateException If a required property is <code>null</code>.
     */
    private ResourceConfig createConfig() throws IllegalStateException {
        
        checkNotNull(submissionManager, "SubmissionManager");
        checkNotNull(storage, "storage");
        checkNotNull(authManager, "AuthManager");
        checkNotNull(stuMgmtView, "StuMgmtView");
        
        ResourceConfig config = new ResourceConfig()
                // routes
                .register(SubmissionRoute.class)
                .register(NotificationRoute.class)
                .register(HeartbeatRoute.class)
                // filters
                .register(CorsFilter.class)
                // exception mappers
                .register(UnauthorizedExceptionMapper.class)
                .register(StorageExceptionMapper.class)
                .register(NoSuchTargetExceptionMapper.class)
                // factories for routes that require constructor parameters
                .register(new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bindFactory(new SubmissionRoute.Factory(submissionManager, storage, authManager))
                                .to(SubmissionRoute.class);
                        bindFactory(new NotificationRoute.Factory(storage, stuMgmtView)).to(NotificationRoute.class);
                    }
                })
                // JSON support
                .register(JsonBindingFeature.class);
        
        LOGGER.fine(() -> "The following resource classes are registered: " + config.getClasses());
        return config;
    }
    
    /**
     * Ensures that the given object is not <code>null</code>.
     * 
     * @param object The object to check.
     * @param name The name of the object to use in the exception message.
     * 
     * @throws IllegalStateException If the object is <code>null</code>.
     */
    private static void checkNotNull(Object object, String name) throws IllegalStateException {
        if (object == null) {
            throw new IllegalStateException("No " + name + " specified");
        }
    }
    
    /**
     * Creates the standard {@link Check}s for all courses. Currently this is only the {@link FileSizeCheck}.
     * 
     * @param submissionManager The manager to add the {@link Check}s to.
     */
    private static void createStandardChecks(SubmissionManager submissionManager) {
        FileSizeCheck fileSizeCheck = new FileSizeCheck();
        fileSizeCheck.setMaxFileSize(1024 * 1024); // 1 MiB
        fileSizeCheck.setMaxSubmissionSize(1024 * 1024); // 1 MiB
        
        submissionManager.addDefaultRejectingCheck(fileSizeCheck);
    }
    
    /**
     * Creates and starts the server with default configuration. Also does the initial loading of the StuMgmtView.
     *  
     * @param port The port to use.
     * @param storagePath The path to the {@link FilesystemStorage} to use.
     * @param authSystemUrl The URL to the authentication system (sparky-service) API.
     * @param stuMgmtUrl The URL to the student management system API.
     * @param username The username to authenticate this service as in the auth sytem.
     * @param password The password to authenticate this service with in the auth system.
     * @param keystorePath The path to the keystore file to use for TLS.
     * @param keystorePassword The password of the keystore file to use for TLS.
     * 
     * @return The started HTTP server.
     * 
     * @throws IOException If creating the {@link FilesystemStorage} fails.
     */
    // checkstyle: stop parameter number check
    public static ExerciseSubmitterServer startDefaultServer(int port, String storagePath, String authSystemUrl,
            String stuMgmtUrl, String username, String password,
            Optional<String> keystorePath, Optional<String> keystorePassword) throws IOException {
    // checkstyle: resume parameter number check
        
        LOGGER.config(() -> "Using storage directory " + storagePath);
        ISubmissionStorage storage = new FilesystemStorage(Path.of(storagePath));
        StuMgmtView stuMgmtView = new StuMgmtView(stuMgmtUrl, authSystemUrl, username, password);
        
        SubmissionManager submissionManager = new SubmissionManager(storage, stuMgmtView);
        createStandardChecks(submissionManager);
        
        AuthManager authManager = new AuthManager(authSystemUrl, stuMgmtView);
        
        ExerciseSubmitterServer server = new ExerciseSubmitterServer();
        server.setPort(port);
        server.setStorage(storage);
        server.setStuMgmtView(stuMgmtView);
        server.setSubmissionManager(submissionManager);
        server.setAuthManager(authManager);
        if (keystorePath.isPresent() && keystorePassword.isPresent()) {
            server.setTlsKeystore(Path.of(keystorePath.get()), keystorePassword.get());
        }
        
        LOGGER.config("Starting HTTP server on port " + port);
        server.start();
        
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
     * Starts the server.
     * 
     * @param args Command line arguments. Content:
     *      <ol>
     *          <li>Port to listen on</li>
     *          <li>Path to storage directory</li>
     *          <li>URL to auth system (Sparky) API</li>
     *          <li>URL to student management system API</li>
     *      </ol>
     * 
     * @throws IOException If creating the submission storage fails (e.g. is not a directory).
     */
    public static void main(String[] args) throws IOException {
        
        LoggingSetup.init();
        LoggingSetup.setLevel("INFO");
        LOGGER.info("Starting");
        
        ExerciseSubmitterServer server = startDefaultServer(Integer.parseInt(args[0]), args[1], args[2], args[3], 
                System.getenv("SUBMISSION_SERVER_MGMT_USER"), System.getenv("SUBMISSION_SERVER_MGMT_PW"),
                Optional.ofNullable(System.getenv("SUBMISSION_SERVER_KEYSTORE_PATH")),
                Optional.ofNullable(System.getenv("SUBMISSION_SERVER_KEYSTORE_PW")));
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down HTTP server");
            server.stop();
        }));
    }

}
