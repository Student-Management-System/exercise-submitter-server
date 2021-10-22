package net.ssehub.teaching.exercise_submitter.server.rest.routes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
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
import net.ssehub.teaching.exercise_submitter.server.rest.dto.VersionDto;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchTargetException;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.Submission;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionBuilder;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
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
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
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
            throw new UnauthorizedException();
        }
        
        String token = authorizationHeader.substring("Bearer ".length());
        
        return submissionManager.authenticate(token);
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
            @ApiResponse(responseCode = "201", description = "Submission successfully stored"),
            @ApiResponse(responseCode = "400", description = "Input data malformed or invalid"),
            @ApiResponse(responseCode = "403", description = "User is not authorized to add a new submission"),
            @ApiResponse(responseCode = "404", description = "Assignment or group does not exist"),
            @ApiResponse(responseCode = "500", description = "An unexpected internal server error occurred")
        },
        security = {
            @SecurityRequirement(name = "bearerAuth")
        }
    )
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/{course}/{assignment}/{group}")
    public Response submit(
            @PathParam("course") String course,
            @PathParam("assignment") String assignmentName,
            @PathParam("group") String groupName,
            @RequestBody(description = "Map of relative file paths (keys) and file content (values)", content = {
                @Content(schema = @Schema(type = "object"), examples = {
                    @ExampleObject(value = "{\"Main.java\": \"file content...\", \"dir/Util.java\": \"content\"}")
                })
            }) Map<String, String> files,
            @HeaderParam("Authorization") @Parameter(hidden = true) String authHeader)
    
            throws NoSuchTargetException, StorageException, UnauthorizedException {
        
        Response response;
        
        try {
            String user = authenticate(authHeader);
            
            SubmissionBuilder builder = new SubmissionBuilder(user);
            for (Map.Entry<String, String> file : files.entrySet()) {
                builder.addFile(java.nio.file.Path.of(file.getKey()), file.getValue());
            }
            
            submissionManager.submit(new SubmissionTarget(course, assignmentName, groupName), builder.build());
            
            response = Response
                    .status(Status.CREATED)
                    .build();
            
        } catch (IllegalArgumentException e) {
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
        },
        security = {
            @SecurityRequirement(name = "bearerAuth")
        }
    )
    @GET
    @Path("/{course}/{assignment}/{group}/versions")
    public Response listVersions(
            @PathParam("course") String course,
            @PathParam("assignment") String assignmentName,
            @PathParam("group") String groupName,
            @HeaderParam("Authorization") @Parameter(hidden = true)  String authHeader)
    
            throws NoSuchTargetException, StorageException, UnauthorizedException {

        String user = authenticate(authHeader);
        
        List<Version> versions = submissionManager.getVersions(
                new SubmissionTarget(course, assignmentName, groupName), user);
        
        List<VersionDto> dtos = versions.stream()
            .map(version -> {
                VersionDto dto = new VersionDto();
                dto.setAuthor(version.getAuthor());
                dto.setTimestamp(version.getUnixTimestamp());
                return dto;
            })
            .collect(Collectors.toList());
        
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
        },
        security = {
            @SecurityRequirement(name = "bearerAuth")
        }
    )
    @GET
    @Path("/{course}/{assignment}/{group}/latest")
    public Response getLatest(
            @PathParam("course") String course,
            @PathParam("assignment") String assignmentName,
            @PathParam("group") String groupName,
            @HeaderParam("Authorization") @Parameter(hidden = true)  String authHeader)
    
            throws NoSuchTargetException, StorageException, UnauthorizedException  {
        
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
        },
        security = {
            @SecurityRequirement(name = "bearerAuth")
        }
    )
    @GET
    @Path("/{course}/{assignment}/{group}/{version}")
    public Response getVersion(
            @PathParam("course") String course,
            @PathParam("assignment") String assignmentName,
            @PathParam("group") String groupName,
            @PathParam("version")
                @Parameter(description = "Identifies the version as a unix timestamp (seconds since epoch)")
                long timestamp,
            @HeaderParam("Authorization") @Parameter(hidden = true)  String authHeader)
    
            throws NoSuchTargetException, StorageException, UnauthorizedException  {
        
        return getSubmission(authHeader, new SubmissionTarget(course, assignmentName, groupName), versions -> {
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
     * @param authHeader The JWT token to authentiate the user.
     * @param target The course, assignment, and group identifier to retrieve the submission of.
     * @param versionSelector A callback function to decide which version to use. The given list always has at least one
     *      item. Return <code>null</code> to indicate that the wanted version is not available.
     *      
     * @return A HTTP response with a {@link SubmissionDto} as data.
     */
    private Response getSubmission(String authHeader, SubmissionTarget target,
            Function<List<Version>, Version> versionSelector)
    
            throws NoSuchTargetException, StorageException, UnauthorizedException {
        
        String user = authenticate(authHeader);
        
        Response response;
        List<Version> versions = submissionManager.getVersions(target, user);
        if (!versions.isEmpty()) {
            
            Version selectedVersion = versionSelector.apply(versions);
            if (selectedVersion != null) {
                Submission submission = submissionManager
                        .getSubmission(target, selectedVersion, user);
                
                Map<String, String> files = new HashMap<>(submission.getNumFiles());
                for (java.nio.file.Path file : submission.getFilepaths()) {
                    files.put(file.toString().replace('\\', '/'), submission.getFileContent(file));
                }
                
                response = Response.ok(files).build();
                
            } else {
                response = Response
                        .status(Status.NOT_FOUND.getStatusCode(), "Selected version not found for group "
                                + target.getGroupName() + " in assignment " + target.getAssignmentName()
                                + " in course " + target.getCourse())
                        .build();
            }
            
        } else {
            response = Response
                    .status(Status.NOT_FOUND.getStatusCode(), "No versions have been submitted for group "
                            + target.getGroupName() + " in assignment " + target.getAssignmentName()
                            + " in course " + target.getCourse())
                    .build();
        }
            
        
        return response;
    }
    
}
