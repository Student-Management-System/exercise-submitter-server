package net.ssehub.teaching.exercise_submitter.server.rest.routes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchAssignmentException;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchGroupException;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.Submission;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionBuilder;
import net.ssehub.teaching.exercise_submitter.server.storage.Version;
import net.ssehub.teaching.exercise_submitter.server.submission.SubmissionManager;
import net.ssehub.teaching.exercise_submitter.server.submission.UnauthorizedException;

/**
 * The route for working with submissions.
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
@Path("/submission")
@Produces(MediaType.APPLICATION_JSON)
public class SubmissionRoute {
    
    private SubmissionManager submissionManager;
    
    /**
     * Creates a new submission route with the given storage.
     * 
     * @param submissionManager The {@link SubmissionManager} to use.
     */
    public SubmissionRoute(SubmissionManager submissionManager) {
        this.submissionManager = submissionManager;
    }
    
    /**
     * Adds a new submission.
     * 
     * @param assignmentName The name of the assignment to submit to.
     * @param groupName The name of the group to submit for.
     * @param files The files of the submission. Key are relative file paths, values are file content.
     * 
     * @return A fitting HTTP response.
     */
    @Operation(
        description = "Adds a new submission for the given assignment and group",
        responses = {
            @ApiResponse(responseCode = "201", description = "Submission successfully stored"),
            @ApiResponse(responseCode = "400", description = "Input data malformed or invalid"),
            @ApiResponse(responseCode = "403", description = "User is not authorized to add a new submission"),
            @ApiResponse(responseCode = "404", description = "Assignment or group does not exist"),
            @ApiResponse(responseCode = "500", description = "An unexpected internal server error occurred")
        }
    )
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{assignment}/{group}")
    public Response submit(
            @PathParam("assignment") String assignmentName,
            @PathParam("group") String groupName,
            @RequestBody(description = "Map of relative file paths (keys) and file content (values)", content = {
                @Content(schema = @Schema(type = "object"), examples = {
                    @ExampleObject(value = "{\"Main.java\": \"file content...\", \"dir/Util.java\": \"content\"}")
                })
            }) Map<String, String> files) {
        
        Response response;
        
        // TODO: authentication
        
        try {
            SubmissionBuilder builder = new SubmissionBuilder();
            for (Map.Entry<String, String> file : files.entrySet()) {
                builder.addFile(java.nio.file.Path.of(file.getKey()), file.getValue());
            }
            
            submissionManager.submit(assignmentName, groupName, "user", builder.build()); // TODO
            
            response = Response
                    .status(Status.CREATED)
                    .build();
            
        } catch (IllegalArgumentException e) {
            response = Response
                    .status(Status.BAD_REQUEST.getStatusCode(), "Invalid filepath: " + e.getMessage())
                    .build();
            
        } catch (UnauthorizedException e) {
            response = Response
                    .status(Status.FORBIDDEN.getStatusCode(), "Unauthorized")
                    .build();
            
        } catch (NoSuchAssignmentException e) {
            response = Response
                    .status(Status.NOT_FOUND.getStatusCode(), "Assignment " + e.getAssignmentName() + " not found")
                    .build();
            
        } catch (NoSuchGroupException e) {
            response = Response
                    .status(Status.NOT_FOUND.getStatusCode(), "Group " + e.getGroupName()
                            + " not found in assignment " + e.getAssignmentName())
                    .build();
            
        } catch (StorageException e) {
            response = Response
                    .status(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Unexpected storage exception: "
                            + e.getMessage())
                    .build();
        }
        
        return response;
    }
    
    /**
     * Retrieves a list of submitted versions.
     * 
     * @param assignmentName The name of the assignment to get.
     * @param groupName The name of the group to get the versions list of.
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
    @Path("/{assignment}/{group}/versions")
    public Response listVersions(
            @PathParam("assignment") String assignmentName,
            @PathParam("group") String groupName) {

        Response response;
        
        // TODO authentication
        
        try {
            List<Version> versions = submissionManager.getVersions(assignmentName, groupName, "user"); // TODO
            
            List<VersionDto> dtos = versions.stream()
                .map(version -> {
                    VersionDto dto = new VersionDto();
                    dto.setAuthor(version.getAuthor());
                    dto.setTimestamp(version.getUnixTimestamp());
                    return dto;
                })
                .collect(Collectors.toList());
            
            response = Response
                    .ok(dtos)
                    .build();
            
        } catch (UnauthorizedException e) {
            response = Response
                    .status(Status.FORBIDDEN.getStatusCode(), "Unauthorized")
                    .build();
            
        } catch (NoSuchAssignmentException e) {
            response = Response
                    .status(Status.NOT_FOUND.getStatusCode(), "Assignment " + e.getAssignmentName() + " not found")
                    .build();
            
        } catch (NoSuchGroupException e) {
            response = Response
                    .status(Status.NOT_FOUND.getStatusCode(), "Group " + e.getGroupName()
                            + " not found in assignment " + e.getAssignmentName())
                    .build();
            
        } catch (StorageException e) {
            response = Response
                    .status(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Unexpected storage exception: "
                            + e.getMessage())
                    .build();
        }
        
        return response;
    }
    
    /**
     * Retrieves the latest version of a submission.
     * 
     * @param assignmentName The name of the assignment to retrieve.
     * @param groupName The name of the group to retrieve the latest submission of.
     * 
     * @return A HTTP response with a {@link SubmissionDto} as data.
     */
    @Operation(
        description = "Retrieves the latest submission of the given assignment and group",
        responses = {
            @ApiResponse(responseCode = "200", description = "Submission is returned", content = {
                @Content(schema = @Schema(type = "object"), examples = {
                    @ExampleObject(value = "{\"Main.java\": \"file content...\", \"dir/Util.java\": \"content\"}")
                })
            }),
            @ApiResponse(responseCode = "403", description = "User is not authorized to retrieve a submission"),
            @ApiResponse(responseCode = "404",
                description = "Assignment or group does not exist, or there is no version to retrieve"),
            @ApiResponse(responseCode = "500", description = "An unexpected internal server error occurred")
        }
    )
    @GET
    @Path("/{assignment}/{group}/latest")
    public Response getLatest(
            @PathParam("assignment") String assignmentName,
            @PathParam("group") String groupName) {
        
        return getSubmission(assignmentName, groupName, versions -> versions.get(0));
    }
    
    /**
     * Retrieves the latest version of a submission.
     * 
     * @param assignmentName The name of the assignment to retrieve.
     * @param groupName The name of the group to retrieve the submission of.
     * @param timestamp The Unix timestamp identifying the version.
     * 
     * @return A HTTP response with a {@link SubmissionDto} as data.
     */
    @Operation(
        description = "Retrieves the specified submission of the given assignment and group",
        responses = {
            @ApiResponse(responseCode = "200", description = "Submission is returned", content = {
                @Content(schema = @Schema(type = "object"), examples = {
                    @ExampleObject(value = "{\"Main.java\": \"file content...\", \"dir/Util.java\": \"content\"}")
                })
            }),
            @ApiResponse(responseCode = "403", description = "User is not authorized to retrieve a submission"),
            @ApiResponse(responseCode = "404",
                description = "Assignment or group does not exist, or the specified version does not exist"),
            @ApiResponse(responseCode = "500", description = "An unexpected internal server error occurred")
        }
    )
    @GET
    @Path("/{assignment}/{group}/{version}")
    public Response getVersion(
            @PathParam("assignment") String assignmentName,
            @PathParam("group") String groupName,
            @PathParam("version")
                @Parameter(description = "Identifies the version as a unix timestamp (seconds since epoch)")
                long timestamp) {
        
        return getSubmission(assignmentName, groupName, versions -> {
            Version match = null;
            for (Version version : versions) {
                if (version.getUnixTimestamp() == timestamp) {
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
     * @param assignmentName The name of the assignment to retrieve.
     * @param groupName The name of the group to retrieve the submission of.
     * @param versionSelector A callback function to decide which version to use. The given list always has at least one
     *      item. Return <code>null</code> to indicate that the wanted version is not available.
     *      
     * @return A HTTP response with a {@link SubmissionDto} as data.
     */
    private Response getSubmission(String assignmentName, String groupName,
            Function<List<Version>, Version> versionSelector) {
        
        Response response;
        
        // TODO authentication
        
        try {
            List<Version> versions = submissionManager.getVersions(assignmentName, groupName, "user"); // TODO
            if (!versions.isEmpty()) {
                
                Version selectedVersion = versionSelector.apply(versions);
                if (selectedVersion != null) {
                    Submission submission = submissionManager
                            .getSubmission(assignmentName, groupName, selectedVersion, "user"); // TODO
                    
                    Map<String, String> files = new HashMap<>(submission.getNumFiles());
                    for (java.nio.file.Path file : submission.getFilepaths()) {
                        files.put(file.toString().replace('\\', '/'), submission.getFileContent(file));
                    }
                    
                    response = Response.ok(files).build();
                    
                } else {
                    response = Response
                            .status(Status.NOT_FOUND.getStatusCode(), "Selected version not found for group "
                                    + groupName + " in assignment " + assignmentName)
                            .build();
                }
                
                
            } else {
                response = Response
                        .status(Status.NOT_FOUND.getStatusCode(), "No versions have been submitted for group "
                                + groupName + " in assignment " + assignmentName)
                        .build();
            }
            
        } catch (UnauthorizedException e) {
            response = Response
                    .status(Status.FORBIDDEN.getStatusCode(), "Unauthorized")
                    .build();
            
        } catch (NoSuchAssignmentException e) {
            response = Response
                    .status(Status.NOT_FOUND.getStatusCode(), "Assignment " + e.getAssignmentName() + " not found")
                    .build();
            
        } catch (NoSuchGroupException e) {
            response = Response
                    .status(Status.NOT_FOUND.getStatusCode(), "Group " + e.getGroupName()
                            + " not found in assignment " + e.getAssignmentName())
                    .build();
            
        } catch (StorageException e) {
            response = Response
                    .status(Status.INTERNAL_SERVER_ERROR.getStatusCode(), "Unexpected storage exception: "
                            + e.getMessage())
                    .build();
        }
        
        return response;
    }
    
}
