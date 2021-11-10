package net.ssehub.teaching.exercise_submitter.server.rest.routes;

import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import net.ssehub.teaching.exercise_submitter.server.auth.AuthManager;
import net.ssehub.teaching.exercise_submitter.server.rest.dto.FileDto;
import net.ssehub.teaching.exercise_submitter.server.rest.dto.SubmissionResultDto;
import net.ssehub.teaching.exercise_submitter.server.rest.dto.VersionDto;
import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchTargetException;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.Submission;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionBuilder;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.storage.Version;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.submission.SubmissionManager;
import net.ssehub.teaching.exercise_submitter.server.submission.UnauthorizedException;

/**
 * The route for working with submissions.
 * 
 * @author Adam
 */
@Path("/submission")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "submission")
@SecurityRequirement(name = "bearerAuth")
public class SubmissionRoute {
    
    /**
     * Global lock to ensure that operations don't interfere with each other. Must be acquired by routes that access
     * the {@link StuMgmtView} or {@link ISubmissionStorage}.
     */
    public static final Object LOCK = new Object();
    
    private static final Logger LOGGER = Logger.getLogger(SubmissionRoute.class.getName());
    
    private SubmissionManager submissionManager;
    
    private ISubmissionStorage storage;
    
    private AuthManager authManager;
    
    /**
     * Creates a new submission route with the given storage.
     * 
     * @param submissionManager The {@link SubmissionManager} to use for new submissions.
     * @param storage The {@link ISubmissionStorage} to use for replaying old versions.
     * @param authManager The {@link AuthManager} to use for authentication and authorization.
     */
    public SubmissionRoute(SubmissionManager submissionManager, ISubmissionStorage storage, AuthManager authManager) {
        this.submissionManager = submissionManager;
        this.storage = storage;
        this.authManager = authManager;
    }
    
    /**
     * Factory for creating instances of {@link SubmissionRoute}.
     */
    public static class Factory implements org.glassfish.hk2.api.Factory<SubmissionRoute> {
        
        private SubmissionManager submissionManager;
        
        private ISubmissionStorage storage;
        
        private AuthManager authManager;
        
        /**
         * Creates a factory with the given parameters.
         * 
         * @param submissionManager The {@link SubmissionManager} to use for new submissions.
         * @param storage The {@link ISubmissionStorage} to use for replaying old versions.
         * @param authManager The {@link AuthManager} to use for authentication and authorization.
         */
        public Factory(SubmissionManager submissionManager, ISubmissionStorage storage, AuthManager authManager) {
            this.submissionManager = submissionManager;
            this.storage = storage;
            this.authManager = authManager;
        }

        @Override
        public SubmissionRoute provide() {
            return new SubmissionRoute(submissionManager, storage, authManager);
        }

        @Override
        public void dispose(SubmissionRoute instance) {
        }
        
    }
    
    
    /**
     * Authenticates the user using the given value of the <code>Authorization</code> HTTP header.
     * 
     * @param authorizationHeader The value of the <code>Authorization</code> HTTP header. Must start with
     *      <code>Bearer</code>.
     *      
     * @return The username of the authenticated user.
     * 
     * @throws UnauthorizedException If the user could not be authenticated.
     */
    private String authenticate(String authorizationHeader) throws UnauthorizedException {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            LOGGER.info(() -> "Missing or invalid authorization header: " + authorizationHeader);
            throw new UnauthorizedException();
        }
        
        String token = authorizationHeader.substring("Bearer ".length());
        
        return authManager.authenticate(token);
    }
    
    /**
     * Adds a new submission.
     * 
     * @param course The identifier of the course of the assignment.
     * @param assignmentName The name of the assignment to submit to.
     * @param groupName The name of the group to submit for.
     * @param files The files of the submission. Key are relative file paths, values are file content.
     * @param authHeader The JWT token to authenticate the user.
     * 
     * @return A fitting HTTP response.
     */
    @Operation(
        description = "Adds a new submission for the given assignment and group",
        responses = {
            @ApiResponse(
                responseCode = "201",
                description = "Submission accepted",
                content = {
                    @Content(
                        schema = @Schema(implementation = SubmissionResultDto.class),
                        examples = {
                            @ExampleObject(value = "{\"accepted\": true, \"messages\": []}")
                        })
                }),
            @ApiResponse(
                responseCode = "200",
                description = "Submission rejected based on submission checks",
                content = {
                    @Content(
                        schema = @Schema(implementation = SubmissionResultDto.class),
                        examples = {
                            @ExampleObject(value = "{\"accepted\": false, \"messages\": []}")
                        })
                }),
            @ApiResponse(responseCode = "400", description = "Input data malformed or invalid"),
            @ApiResponse(responseCode = "403", description = "User is not authorized to add a new submission"),
            @ApiResponse(responseCode = "404", description = "Assignment or group does not exist"),
            @ApiResponse(responseCode = "500", description = "An unexpected internal server error occurred")
        }
    )
    @POST
    @Path("/{course}/{assignment}/{group}")
    public Response submit(
            @PathParam("course")
            @Parameter(description = "ID of the course that contains the assignment")
            String course,
            
            @PathParam("assignment")
            @Parameter(description = "Name of the assignment to submit to")
            String assignmentName,
            
            @PathParam("group")
            @Parameter(description = "Name of the group (or username for single assignments) to submit to")
            String groupName,
            
            @RequestBody(description = "The files of this submission")
            List<FileDto> files,
            
            @HeaderParam("Authorization")
            @Parameter(hidden = true)
            String authHeader)
    
            throws NoSuchTargetException, StorageException, UnauthorizedException {
        
        LOGGER.info(() -> "Submission request to " + course + "/" + assignmentName + "/" + groupName + " received");
        
        Response response;
        SubmissionTarget target = new SubmissionTarget(course, assignmentName, groupName);
        
        try {
            String user = authenticate(authHeader);
            
            SubmissionResultDto result;
            
            synchronized (LOCK) {
                authManager.checkSubmissionAllowed(user, target);
                
                SubmissionBuilder builder = new SubmissionBuilder(user);
                for (FileDto file : files) {
                    builder.addFile(java.nio.file.Path.of(file.getPath()),
                            Base64.getDecoder().decode(file.getContent()));
                }
                
                result = submissionManager.submit(target, builder.build());
            }
            
            response = Response
                    .status(result.getAccepted() ? Status.CREATED : Status.OK)
                    .entity(result)
                    .build();
            
        } catch (IllegalArgumentException e) {
            LOGGER.info(() -> "Invalid filepath in submission: " + e.getMessage());
            
            response = Response
                    .status(Status.BAD_REQUEST.getStatusCode(), "Invalid filepath: " + e.getMessage())
                    .build();
        }
        
        return response;
    }
    
    /**
     * Retrieves a list of submitted versions.
     * 
     * @param course The identifier of the course of the assignment.
     * @param assignmentName The name of the assignment to get.
     * @param groupName The name of the group to get the versions list of.
     * @param authHeader The JWT token to authenticate the user.
     * 
     * @return A HTTP response with a list of {@link VersionDto}s.
     */
    @Operation(
        description = "Retrieves a list of all submitted versions for the given assignment and group",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of versions is returned", content = {
                @Content(array = @ArraySchema(schema = @Schema(implementation = VersionDto.class)))
            }),
            @ApiResponse(responseCode = "403", description = "User is not authorized to get the version list"),
            @ApiResponse(responseCode = "404", description = "Assignment or group does not exist"),
            @ApiResponse(responseCode = "500", description = "An unexpected internal server error occurred")
        }
    )
    @GET
    @Path("/{course}/{assignment}/{group}/versions")
    public Response listVersions(
            @PathParam("course")
            @Parameter(description = "ID of the course that contains the assignment")
            String course,
            
            @PathParam("assignment")
            @Parameter(description = "Name of the assignment to get versions for")
            String assignmentName,
            
            @PathParam("group")
            @Parameter(description = "Name of the group (or username for single assignments) to get versions for")
            String groupName,
            
            @HeaderParam("Authorization")
            @Parameter(hidden = true)
            String authHeader)
    
            throws NoSuchTargetException, StorageException, UnauthorizedException {

        LOGGER.info(() -> "Request to list versions of " + course + "/" + assignmentName + "/" + groupName
                + " received");

        SubmissionTarget target = new SubmissionTarget(course, assignmentName, groupName);
        
        String user = authenticate(authHeader);
        
        List<Version> versions;
        
        synchronized (LOCK) {
            authManager.checkReplayAllowed(user, target);
            versions = storage.getVersions(target);
        }
        
        List<VersionDto> dtos = versions.stream()
            .map(version -> {
                VersionDto dto = new VersionDto();
                dto.setAuthor(version.getAuthor());
                dto.setTimestamp(version.getCreationTime().getEpochSecond());
                return dto;
            })
            .collect(Collectors.toList());
        
        LOGGER.info(() -> "Returning list of " + dtos.size() + " versions");
        
        return Response
                .ok(dtos)
                .build();
    }
    
    /**
     * Retrieves the latest version of a submission.
     * 
     * @param course The identifier of the course of the assignment.
     * @param assignmentName The name of the assignment to retrieve.
     * @param groupName The name of the group to retrieve the latest submission of.
     * @param authHeader The JWT token to authenticate the user.
     * 
     * @return A HTTP response with a list of {@link FileDto} as data.
     */
    @Operation(
        description = "Retrieves the latest submission of the given assignment and group",
        responses = {
            @ApiResponse(responseCode = "200", description = "Submission is returned", content = {
                @Content(array = @ArraySchema(schema = @Schema(implementation = FileDto.class)))}),
            @ApiResponse(responseCode = "403", description = "User is not authorized to retrieve a submission"),
            @ApiResponse(responseCode = "404",
                description = "Assignment or group does not exist, or there is no version to retrieve"),
            @ApiResponse(responseCode = "500", description = "An unexpected internal server error occurred")
        }
    )
    @GET
    @Path("/{course}/{assignment}/{group}/latest")
    public Response getLatest(
            @PathParam("course")
            @Parameter(description = "ID of the course that contains the assignment")
            String course,
            
            @PathParam("assignment")
            @Parameter(description = "Name of the assignment to retrieve from")
            String assignmentName,
            
            @PathParam("group")
            @Parameter(description = "Name of the group (or username for single assignments) to retrieve from")
            String groupName,
            
            @HeaderParam("Authorization")
            @Parameter(hidden = true)
            String authHeader)
    
            throws NoSuchTargetException, StorageException, UnauthorizedException  {
        
        LOGGER.info(() -> "Replay of latest version of " + course + "/" + assignmentName + "/" + groupName
                + " received");
        
        return getSubmission(authHeader, new SubmissionTarget(course, assignmentName, groupName),
            versions -> versions.get(0));
    }
    
    /**
     * Retrieves the latest version of a submission.
     * 
     * @param course The identifier of the course of the assignment.
     * @param assignmentName The name of the assignment to retrieve.
     * @param groupName The name of the group to retrieve the submission of.
     * @param timestamp The Unix timestamp identifying the version.
     * @param authHeader The JWT token to authenticate the user.
     * 
     * @return A HTTP response with a list of {@link FileDto} as data.
     */
    @Operation(
        description = "Retrieves the specified submission of the given assignment and group",
        responses = {
            @ApiResponse(responseCode = "200", description = "Submission is returned", content = {
                @Content(array = @ArraySchema(schema = @Schema(implementation = FileDto.class)))}),
            @ApiResponse(responseCode = "403", description = "User is not authorized to retrieve a submission"),
            @ApiResponse(responseCode = "404",
                description = "Assignment or group does not exist, or the specified version does not exist"),
            @ApiResponse(responseCode = "500", description = "An unexpected internal server error occurred")
        }
    )
    @GET
    @Path("/{course}/{assignment}/{group}/{version}")
    public Response getVersion(
            @PathParam("course")
            @Parameter(description = "ID of the course that contains the assignment")
            String course,
            
            @PathParam("assignment")
            @Parameter(description = "Name of the assignment to retrieve from")
            String assignmentName,
            
            @PathParam("group")
            @Parameter(description = "Name of the group (or username for single assignments) to retrieve from")
            String groupName,
            
            @PathParam("version")
            @Parameter(description = "Identifies the version as a unix timestamp (seconds since epoch)")
            long timestamp,
            
            @HeaderParam("Authorization")
            @Parameter(hidden = true)
            String authHeader)
    
            throws NoSuchTargetException, StorageException, UnauthorizedException  {
        
        LOGGER.info(() -> "Replay of version " + timestamp + " of " + course + "/" + assignmentName + "/" + groupName
                + " received");
        
        return getSubmission(authHeader, new SubmissionTarget(course, assignmentName, groupName), versions -> {
            Version match = null;
            for (Version version : versions) {
                if (version.getCreationTime().getEpochSecond() == timestamp) {
                    match = version;
                    break;
                }
            }
            return match;
        });
    }
    
    /**
     * Retrieves a submission. A callback is used to let the caller decide which version to retrieve.
     * 
     * @param authHeader The JWT token to authentiate the user.
     * @param target The course, assignment, and group identifier to retrieve the submission of.
     * @param versionSelector A callback function to decide which version to use. The given list always has at least one
     *      item. Return <code>null</code> to indicate that the wanted version is not available.
     *      
     * @return A HTTP response with a list of {@link FileDto} as data.
     */
    private Response getSubmission(String authHeader, SubmissionTarget target,
            Function<List<Version>, Version> versionSelector)
    
            throws NoSuchTargetException, StorageException, UnauthorizedException {
        
        String user = authenticate(authHeader);
        
        Response response;
        synchronized (LOCK) {
            authManager.checkReplayAllowed(user, target);
            
            List<Version> versions = storage.getVersions(target);
            if (!versions.isEmpty()) {
                
                Version selectedVersion = versionSelector.apply(versions);
                if (selectedVersion != null) {
                    Submission submission = storage.getSubmission(target, selectedVersion);
                    
                    List<FileDto> files = new LinkedList<>();
                    
                    for (java.nio.file.Path filepath : submission.getFilepaths()) {
                        files.add(new FileDto(
                                filepath.toString().replace('\\', '/'),
                                submission.getFileContent(filepath)));
                    }
                    
                    LOGGER.info(() -> "Returning submission version "
                            + selectedVersion.getCreationTime().getEpochSecond() + " by " + submission.getAuthor());
                    
                    response = Response.ok(files).build();
                    
                } else {
                    LOGGER.info(() -> "Requested version does not exist");
                    
                    response = Response
                            .status(Status.NOT_FOUND.getStatusCode(), "Selected version not found for group "
                                    + target.getGroupName() + " in assignment " + target.getAssignmentName()
                                    + " in course " + target.getCourse())
                            .build();
                }
                
            } else {
                LOGGER.info(() -> "No version yet submitted");
                
                response = Response
                        .status(Status.NOT_FOUND.getStatusCode(), "No versions have been submitted for group "
                                + target.getGroupName() + " in assignment " + target.getAssignmentName()
                                + " in course " + target.getCourse())
                        .build();
            }
        }
        
        return response;
    }
    
}
