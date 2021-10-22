package net.ssehub.teaching.exercise_submitter.server.rest.routes;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.ssehub.teaching.exercise_submitter.server.storage.EmptyStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchAssignmentException;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchGroupException;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchVersionException;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.Submission;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionBuilder;
import net.ssehub.teaching.exercise_submitter.server.storage.Version;
import net.ssehub.teaching.exercise_submitter.server.submission.SubmissionManagerMock;
import net.ssehub.teaching.exercise_submitter.server.submission.UnauthorizedException;

public class SubmissionRouteTest extends AbstractRestTest {

    @Test
    public void submitInvalidFilepathBadRequest() {
        Response response = target.path("/submission/Homework01/Group01").request()
                .post(Entity.entity(Map.of("../test.txt", "some content\n"), MediaType.APPLICATION_JSON));
        
        assertAll(
            () -> assertEquals(400, response.getStatus()),
            () -> assertEquals("Invalid filepath: .. is not allowed in submission paths",
                    response.getStatusInfo().getReasonPhrase())
        );
    }
    
    @Test
    public void submitForbidden() {
        setManager(new SubmissionManagerMock(new EmptyStorage()) {
            @Override
            public void submit(String assignmentName, String groupName, String author, Submission submission)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException, UnauthorizedException {
                throw new UnauthorizedException();
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01").request()
                .post(Entity.entity(Map.of("test.txt", "some content\n"), MediaType.APPLICATION_JSON));
        
        assertAll(
            () -> assertEquals(403, response.getStatus()),
            () -> assertEquals("Unauthorized", response.getStatusInfo().getReasonPhrase())
        );
    }
    
    @Test
    public void submitNonExistingAssignmentNotFound() {
        setStorage(new EmptyStorage() {
            @Override
            public void submitNewVersion(String assignmentName, String groupName, String author, Submission submission)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
                throw new NoSuchAssignmentException(assignmentName);
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01").request()
                .post(Entity.entity(Map.of("test.txt", "some content\n"), MediaType.APPLICATION_JSON));
        
        assertAll(
            () -> assertEquals(404, response.getStatus()),
            () -> assertEquals("Assignment Homework01 not found", response.getStatusInfo().getReasonPhrase())
        );
    }
    
    @Test
    public void submitNonExistingGroupNotFound() {
        setStorage(new EmptyStorage() {
            @Override
            public void submitNewVersion(String assignmentName, String groupName, String author, Submission submission)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
                throw new NoSuchGroupException(assignmentName, groupName);
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01").request()
                .post(Entity.entity(Map.of("test.txt", "some content\n"), MediaType.APPLICATION_JSON));
        
        assertAll(
            () -> assertEquals(404, response.getStatus()),
            () -> assertEquals("Group Group01 not found in assignment Homework01",
                    response.getStatusInfo().getReasonPhrase())
        );
    }
    
    @Test
    public void submitStorageExceptionInternalServerError() {
        setStorage(new EmptyStorage() {
            @Override
            public void submitNewVersion(String assignmentName, String groupName, String author, Submission submission)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
                throw new StorageException("mock");
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01").request()
                .post(Entity.entity(Map.of("test.txt", "some content\n"), MediaType.APPLICATION_JSON));
        
        assertAll(
            () -> assertEquals(500, response.getStatus()),
            () -> assertEquals("Unexpected storage exception: mock", response.getStatusInfo().getReasonPhrase())
        );
    }
    
    @Test
    public void validSubmissionStored() {
        AtomicReference<Submission> result = new AtomicReference<>();
        
        setStorage(new EmptyStorage() {
            @Override
            public void submitNewVersion(String assignmentName, String groupName, String author, Submission submission)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
                result.set(submission);
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01").request()
                .post(Entity.entity(Map.of("test.txt", "some content\n"), MediaType.APPLICATION_JSON));
        
        assertAll(
            () -> assertEquals(201, response.getStatus()),
            () -> assertEquals(1, result.get().getNumFiles()),
            () -> assertEquals("some content\n", result.get().getFileContent(Path.of("test.txt")))
        );
    }
    
    @Test
    public void listVersionsForbidden() {
        setManager(new SubmissionManagerMock(new EmptyStorage()) {
            @Override
            public List<Version> getVersions(String assignmentName, String groupName, String author)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException, UnauthorizedException {
                throw new UnauthorizedException();
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01/versions").request().get();
        
        assertAll(
            () -> assertEquals(403, response.getStatus()),
            () -> assertEquals("Unauthorized", response.getStatusInfo().getReasonPhrase())
        );
    }
    
    @Test
    public void listVersionsNonExistingAssignmentNotFound() {
        setStorage(new EmptyStorage() {
            
            @Override
            public List<Version> getVersions(String assignmentName, String groupName)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
                throw new NoSuchAssignmentException(assignmentName);
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01/versions").request().get();
        
        assertAll(
            () -> assertEquals(404, response.getStatus()),
            () -> assertEquals("Assignment Homework01 not found", response.getStatusInfo().getReasonPhrase())
        );
    }
    
    @Test
    public void listVersionsNonExistingGroupNotFound() {
        setStorage(new EmptyStorage() {
            @Override
            public List<Version> getVersions(String assignmentName, String groupName)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
                throw new NoSuchGroupException(assignmentName, groupName);
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01/versions").request().get();
        
        assertAll(
            () -> assertEquals(404, response.getStatus()),
            () -> assertEquals("Group Group01 not found in assignment Homework01",
                    response.getStatusInfo().getReasonPhrase())
        );
    }
    
    @Test
    public void listVersionsStorageExceptionInternalServerError() {
        setStorage(new EmptyStorage() {
            @Override
            public List<Version> getVersions(String assignmentName, String groupName)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
                throw new StorageException("mock");
            }
        });

        Response response = target.path("/submission/Homework01/Group01/versions").request().get();
        
        assertAll(
            () -> assertEquals(500, response.getStatus()),
            () -> assertEquals("Unexpected storage exception: mock", response.getStatusInfo().getReasonPhrase())
        );
    }
    
    @Test
    public void listVersionsNoversion() {
        setStorage(new EmptyStorage() {
            @Override
            public List<Version> getVersions(String assignmentName, String groupName)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
                return Arrays.asList();
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01/versions").request().get();
        
        assertAll(
            () -> assertEquals(200, response.getStatus()),
            () -> assertEquals(Collections.emptyList(), response.readEntity(List.class))
        );
    }
    
    @Test
    public void listVersionsMultipleVersions() {
        // unix timestamp 1634831371
        LocalDateTime t1 =  LocalDateTime.ofInstant(Instant.parse("2021-10-21T15:49:31Z"), ZoneId.systemDefault());
        // unix timestamp 1634606632
        LocalDateTime t2 =  LocalDateTime.ofInstant(Instant.parse("2021-10-19T01:23:52Z"), ZoneId.systemDefault());
        
        setStorage(new EmptyStorage() {
            @Override
            public List<Version> getVersions(String assignmentName, String groupName)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
                return Arrays.asList(
                    new Version("tommy", t1),
                    new Version("max", t2)
                );
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01/versions").request().get();
        
        List<?> result = response.readEntity(List.class);
        
        assertAll(
            () -> assertEquals(200, response.getStatus()),
            () -> assertEquals(Arrays.asList(
                    Map.of("author", "tommy", "timestamp", new BigDecimal(1634831371L)),
                    Map.of("author", "max", "timestamp", new BigDecimal(1634606632L))
                ), result)
        );
    }
    
    @Test
    public void getLatestForbidden() {
        setManager(new SubmissionManagerMock(new EmptyStorage()) {
            @Override
            public List<Version> getVersions(String assignmentName, String groupName, String author)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException, UnauthorizedException {
                throw new UnauthorizedException();
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01/latest").request().get();
        
        assertAll(
            () -> assertEquals(403, response.getStatus()),
            () -> assertEquals("Unauthorized", response.getStatusInfo().getReasonPhrase())
        );
    }
    
    @Test
    public void getLatestNonExistingAssignmentNotFound() {
        setStorage(new EmptyStorage() {
            
            @Override
            public List<Version> getVersions(String assignmentName, String groupName)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
                throw new NoSuchAssignmentException(assignmentName);
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01/latest").request().get();
        
        assertAll(
            () -> assertEquals(404, response.getStatus()),
            () -> assertEquals("Assignment Homework01 not found", response.getStatusInfo().getReasonPhrase())
        );
    }
    
    @Test
    public void getLatestNonExistingGroupNotFound() {
        setStorage(new EmptyStorage() {
            @Override
            public List<Version> getVersions(String assignmentName, String groupName)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
                throw new NoSuchGroupException(assignmentName, groupName);
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01/latest").request().get();
        
        assertAll(
            () -> assertEquals(404, response.getStatus()),
            () -> assertEquals("Group Group01 not found in assignment Homework01",
                    response.getStatusInfo().getReasonPhrase())
        );
    }
    
    @Test
    public void getLatestStorageExceptionInternalServerError() {
        setStorage(new EmptyStorage() {
            @Override
            public List<Version> getVersions(String assignmentName, String groupName)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
                throw new StorageException("mock");
            }
        });

        Response response = target.path("/submission/Homework01/Group01/latest").request().get();
        
        assertAll(
            () -> assertEquals(500, response.getStatus()),
            () -> assertEquals("Unexpected storage exception: mock", response.getStatusInfo().getReasonPhrase())
        );
    }
    
    @Test
    public void getLatestNoVersions() {
        Response response = target.path("/submission/Homework01/Group01/latest").request().get();
        
        assertAll(
            () -> assertEquals(404, response.getStatus()),
            () -> assertEquals("No versions have been submitted for group Group01 in assignment Homework01",
                    response.getStatusInfo().getReasonPhrase())
        );
    }
    
    @Test
    public void getLatestSingleVersionWithDirectory() {
        setStorage(new EmptyStorage() {
            @Override
            public List<Version> getVersions(String assignmentName, String groupName)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
                return Arrays.asList(new Version("student123", LocalDateTime.now()));
            }
            
            @Override
            public Submission getSubmission(String assignmentName, String groupName, Version version)
                    throws NoSuchAssignmentException, NoSuchGroupException, NoSuchVersionException, StorageException {
                
                if (!version.getAuthor().equals("student123")) {
                    throw new StorageException();
                }
                
                SubmissionBuilder builder = new SubmissionBuilder();
                builder.addFile(Path.of("dir/test.txt"), "Some content.\n");
                
                return builder.build();
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01/latest").request().get();
        Map<?, ?> result = response.readEntity(Map.class);
        
        assertAll(
            () -> assertEquals(200, response.getStatus()),
            () -> assertEquals(Map.of("dir/test.txt", "Some content.\n"), result)
        );
    }
    
    @Test
    public void getLatestMultipleVersions() {
        setStorage(new EmptyStorage() {
            @Override
            public List<Version> getVersions(String assignmentName, String groupName)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
                return Arrays.asList(
                        new Version("student123", LocalDateTime.now()),
                        new Version("student321", LocalDateTime.now())
                );
            }
            
            @Override
            public Submission getSubmission(String assignmentName, String groupName, Version version)
                    throws NoSuchAssignmentException, NoSuchGroupException, NoSuchVersionException, StorageException {
                
                SubmissionBuilder builder = new SubmissionBuilder();
                builder.addFile(Path.of("author.txt"), version.getAuthor());
                return builder.build();
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01/latest").request().get();
        Map<?, ?> result = response.readEntity(Map.class);
        
        assertAll(
            () -> assertEquals(200, response.getStatus()),
            () -> assertEquals("student123", result.get("author.txt"))
        );
    }
    
    @Test
    public void getVersionNonExistingVersionNotFound() {
        // unix timestamp 1634831371
        LocalDateTime t1 =  LocalDateTime.ofInstant(Instant.parse("2021-10-21T15:49:31Z"), ZoneId.systemDefault());
        // unix timestamp 1634606632
        LocalDateTime t2 =  LocalDateTime.ofInstant(Instant.parse("2021-10-19T01:23:52Z"), ZoneId.systemDefault());
        
        setStorage(new EmptyStorage() {
            @Override
            public List<Version> getVersions(String assignmentName, String groupName)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
                return Arrays.asList(
                    new Version("tommy", t1),
                    new Version("max", t2)
                );
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01/123456").request().get();
        
        assertAll(
            () -> assertEquals(404, response.getStatus()),
            () -> assertEquals("Selected version not found for group Group01 in assignment Homework01",
                    response.getStatusInfo().getReasonPhrase())
        );
    }
    
    @Test
    public void getVersionSelectFirst() {
        // unix timestamp 1634831371
        LocalDateTime t1 =  LocalDateTime.ofInstant(Instant.parse("2021-10-21T15:49:31Z"), ZoneId.systemDefault());
        // unix timestamp 1634606632
        LocalDateTime t2 =  LocalDateTime.ofInstant(Instant.parse("2021-10-19T01:23:52Z"), ZoneId.systemDefault());
        
        setStorage(new EmptyStorage() {
            @Override
            public List<Version> getVersions(String assignmentName, String groupName)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
                return Arrays.asList(
                        new Version("tommy", t1),
                        new Version("max", t2)
                );
            }
            
            @Override
            public Submission getSubmission(String assignmentName, String groupName, Version version)
                    throws NoSuchAssignmentException, NoSuchGroupException, NoSuchVersionException, StorageException {
                SubmissionBuilder builder = new SubmissionBuilder();
                builder.addFile(Path.of("author.txt"), version.getAuthor());
                return builder.build();
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01/1634831371").request().get();
        Map<?, ?> result = response.readEntity(Map.class);
        
        assertAll(
            () -> assertEquals(200, response.getStatus()),
            () -> assertEquals("tommy", result.get("author.txt"))
        );
    }
    
    @Test
    public void getVersionSelectSecond() {
        // unix timestamp 1634831371
        LocalDateTime t1 =  LocalDateTime.ofInstant(Instant.parse("2021-10-21T15:49:31Z"), ZoneId.systemDefault());
        // unix timestamp 1634606632
        LocalDateTime t2 =  LocalDateTime.ofInstant(Instant.parse("2021-10-19T01:23:52Z"), ZoneId.systemDefault());
        
        setStorage(new EmptyStorage() {
            @Override
            public List<Version> getVersions(String assignmentName, String groupName)
                    throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
                return Arrays.asList(
                        new Version("tommy", t1),
                        new Version("max", t2)
                    );
            }
            
            @Override
            public Submission getSubmission(String assignmentName, String groupName, Version version)
                    throws NoSuchAssignmentException, NoSuchGroupException, NoSuchVersionException, StorageException {
                SubmissionBuilder builder = new SubmissionBuilder();
                builder.addFile(Path.of("author.txt"), version.getAuthor());
                return builder.build();
            }
        });
        
        Response response = target.path("/submission/Homework01/Group01/1634606632").request().get();
        Map<?, ?> result = response.readEntity(Map.class);
        
        assertAll(
            () -> assertEquals(200, response.getStatus()),
            () -> assertEquals("max", result.get("author.txt"))
        );
    }
    
}
