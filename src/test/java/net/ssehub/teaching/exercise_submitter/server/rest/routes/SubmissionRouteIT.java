package net.ssehub.teaching.exercise_submitter.server.rest.routes;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.ssehub.studentmgmt.backend_api.ApiException;
import net.ssehub.teaching.exercise_submitter.server.auth.PermissiveAuthManager;
import net.ssehub.teaching.exercise_submitter.server.rest.dto.CheckMessageDto;
import net.ssehub.teaching.exercise_submitter.server.rest.dto.FileDto;
import net.ssehub.teaching.exercise_submitter.server.rest.dto.SubmissionResultDto;
import net.ssehub.teaching.exercise_submitter.server.storage.EmptyStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchTargetException;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.Submission;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionBuilder;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.storage.Version;
import net.ssehub.teaching.exercise_submitter.server.submission.NoChecksSubmissionManager;
import net.ssehub.teaching.exercise_submitter.server.submission.UnauthorizedException;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

public class SubmissionRouteIT extends AbstractRestTest {

    private static final String JWT_TOKEN = "Bearer 123";
    
    private static final String GENERATED_USERNAME = PermissiveAuthManager.generateUsername("123");
    
    @Nested
    public class Submit {
        
        @Test
        public void noTokenUnauthorized() {
            startServer();
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01")
                    .request()
                    .post(Entity.entity(Arrays.asList(), MediaType.APPLICATION_JSON));
            
            assertAll(
                () -> assertEquals(403, response.getStatus()),
                () -> assertEquals("Unauthorized", response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void invalidFilepathBadRequest() {
            startServer();
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .post(Entity.entity(Arrays.asList(new FileDto("../test.txt", "some content\n")), MediaType.APPLICATION_JSON));
            
            assertAll(
                () -> assertEquals(400, response.getStatus()),
                () -> assertEquals("Invalid filepath: .. is not allowed in submission paths",
                        response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void notAuthenticated() throws ApiException {
            setAuthManager(new PermissiveAuthManager() {
                @Override
                public String authenticate(String token) throws UnauthorizedException {
                    throw new UnauthorizedException();
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .post(Entity.entity(Arrays.asList(new FileDto("test.txt", "some content\n")), MediaType.APPLICATION_JSON));
            
            assertAll(
                () -> assertEquals(403, response.getStatus()),
                () -> assertEquals("Unauthorized", response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void notAuthorized() throws ApiException {
            setAuthManager(new PermissiveAuthManager() {
                @Override
                public void checkSubmissionAllowed(String user, SubmissionTarget target) throws UnauthorizedException {
                    if (user.equals(GENERATED_USERNAME)) {
                        throw new UnauthorizedException();
                    }
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .post(Entity.entity(Arrays.asList(new FileDto("test.txt", "some content\n")), MediaType.APPLICATION_JSON));
            
            assertAll(
                () -> assertEquals(403, response.getStatus()),
                () -> assertEquals("Unauthorized", response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void nonExistingTargetNotFound() {
            setSubmissionManager(new NoChecksSubmissionManager(new EmptyStorage() {
                @Override
                public void submitNewVersion(SubmissionTarget target, Submission submission)
                        throws NoSuchTargetException, StorageException {
                    throw new NoSuchTargetException(target);
                }
            }));
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .post(Entity.entity(Arrays.asList(new FileDto("test.txt", "some content\n")), MediaType.APPLICATION_JSON));
            
            assertAll(
                () -> assertEquals(404, response.getStatus()),
                () -> assertEquals("The group Group01 for assignment Homework01 in course foo-wise2122 does not exist",
                        response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void storageExceptionInternalServerError() {
            setSubmissionManager(new NoChecksSubmissionManager(new EmptyStorage() {
                @Override
                public void submitNewVersion(SubmissionTarget target, Submission submission)
                        throws NoSuchTargetException, StorageException {
                    throw new StorageException("mock");
                }
            }));
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .post(Entity.entity(Arrays.asList(new FileDto("test.txt", "some content\n")), MediaType.APPLICATION_JSON));
            
            assertAll(
                () -> assertEquals(500, response.getStatus()),
                () -> assertEquals("Unexpected storage exception: mock", response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void submissionAccepted() {
            AtomicReference<Submission> result = new AtomicReference<>();
            
            setSubmissionManager(new NoChecksSubmissionManager(new EmptyStorage() {
                @Override
                public void submitNewVersion(SubmissionTarget target, Submission submission)
                        throws NoSuchTargetException, StorageException {
                    result.set(submission);
                }
            }));
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .post(Entity.entity(Arrays.asList(new FileDto("test.txt", "some content\n")), MediaType.APPLICATION_JSON));
            
            SubmissionResultDto dto = response.readEntity(SubmissionResultDto.class);
            
            assertAll(
                () -> assertEquals(201, response.getStatus()),
                () -> assertEquals(1, result.get().getNumFiles()),
                () -> assertArrayEquals("some content\n".getBytes(StandardCharsets.UTF_8),
                        result.get().getFileContent(Path.of("test.txt"))),
                () -> assertTrue(dto.getAccepted())
            );
        }
        
        @Test
        public void submissionNotAccepted() {
            setSubmissionManager(new NoChecksSubmissionManager(new EmptyStorage()) {
                @Override
                public SubmissionResultDto submit(SubmissionTarget target, Submission submission)
                        throws NoSuchTargetException, StorageException {
                    SubmissionResultDto result = new SubmissionResultDto();
                    result.setAccepted(false);
                    return result;
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .post(Entity.entity(Arrays.asList(new FileDto("test.txt", "some content\n")), MediaType.APPLICATION_JSON));
            

            SubmissionResultDto result = response.readEntity(SubmissionResultDto.class);
            
            assertAll(
                () -> assertEquals(200, response.getStatus()),
                () -> assertFalse(result.getAccepted())
            );
        }
        
        @Test
        public void submissionReturnsCheckMessages() {
            setSubmissionManager(new NoChecksSubmissionManager(new EmptyStorage()) {
                @Override
                public SubmissionResultDto submit(SubmissionTarget target, Submission submission)
                        throws NoSuchTargetException, StorageException {
                    
                    SubmissionResultDto result = new SubmissionResultDto();
                    result.setAccepted(true);
                    result.setMessages(Arrays.asList(new CheckMessageDto("some-tool", MessageType.ERROR, "foo")));
                    return result;
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .post(Entity.entity(Arrays.asList(new FileDto("test.txt", "some content\n")), MediaType.APPLICATION_JSON));
            
            SubmissionResultDto dto = response.readEntity(SubmissionResultDto.class);
            
            assertAll(
                () -> assertEquals(201, response.getStatus()),
                () -> assertTrue(dto.getAccepted()),
                () -> assertEquals(
                        Arrays.asList(new CheckMessageDto("some-tool", MessageType.ERROR, "foo")), dto.getMessages())
            );
        }
        
    }
    
    @Nested
    public class ListVersions {
        
        @Test
        public void noTokenUnauthorized() {
            startServer();
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/versions")
                    .request()
                    .get();
            
            assertAll(
                () -> assertEquals(403, response.getStatus()),
                () -> assertEquals("Unauthorized", response.getStatusInfo().getReasonPhrase())
            );
        }
    
        @Test
        public void notAuthenticated() throws ApiException {
            setAuthManager(new PermissiveAuthManager() {
                @Override
                public String authenticate(String token) throws UnauthorizedException {
                    throw new UnauthorizedException();
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/versions")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            
            assertAll(
                () -> assertEquals(403, response.getStatus()),
                () -> assertEquals("Unauthorized", response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void notAuthorized() throws ApiException {
            setAuthManager(new PermissiveAuthManager() {
                @Override
                public void checkReplayAllowed(String user, SubmissionTarget target) throws UnauthorizedException {
                    if (user.equals(GENERATED_USERNAME)) {
                        throw new UnauthorizedException();
                    }
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/versions")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            
            assertAll(
                () -> assertEquals(403, response.getStatus()),
                () -> assertEquals("Unauthorized", response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void nonExistingTargetNotFound() {
            setStorage(new EmptyStorage() {
                @Override
                public List<Version> getVersions(SubmissionTarget target)
                        throws NoSuchTargetException, StorageException {
                    throw new NoSuchTargetException(target);
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/versions")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            
            assertAll(
                () -> assertEquals(404, response.getStatus()),
                () -> assertEquals("The group Group01 for assignment Homework01 in course foo-wise2122 does not exist",
                        response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void storageExceptionInternalServerError() {
            setStorage(new EmptyStorage() {
                @Override
                public List<Version> getVersions(SubmissionTarget target)
                        throws NoSuchTargetException, StorageException {
                    throw new StorageException("mock");
                }
            });
            startServer();
    
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/versions")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            
            assertAll(
                () -> assertEquals(500, response.getStatus()),
                () -> assertEquals("Unexpected storage exception: mock", response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void noversion() {
            setStorage(new EmptyStorage() {
                @Override
                public List<Version> getVersions(SubmissionTarget target)
                        throws NoSuchTargetException, StorageException {
                    return Arrays.asList();
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/versions")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            
            assertAll(
                () -> assertEquals(200, response.getStatus()),
                () -> assertEquals(Collections.emptyList(), response.readEntity(List.class))
            );
        }
        
        @Test
        public void multipleVersions() {
            Instant t1 = Instant.ofEpochSecond(1634831371L);
            Instant t2 = Instant.ofEpochSecond(1634606632L);
            
            setStorage(new EmptyStorage() {
                @Override
                public List<Version> getVersions(SubmissionTarget target)
                        throws NoSuchTargetException, StorageException {
                    return Arrays.asList(
                        new Version("tommy", t1),
                        new Version("max", t2)
                    );
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/versions")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            
            List<?> result = response.readEntity(List.class);
            
            assertAll(
                () -> assertEquals(200, response.getStatus()),
                () -> assertEquals(Arrays.asList(
                        Map.of("author", "tommy", "timestamp", new BigDecimal(1634831371L)),
                        Map.of("author", "max", "timestamp", new BigDecimal(1634606632L))
                    ), result)
            );
        }
        
    }
    
    @Nested
    public class GetLatest {
        
        @Test
        public void noTokenUnauthorized() {
            startServer();
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/latest")
                    .request()
                    .get();
            
            assertAll(
                () -> assertEquals(403, response.getStatus()),
                () -> assertEquals("Unauthorized", response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void notAuthenticated() throws ApiException {
            setAuthManager(new PermissiveAuthManager() {
                @Override
                public String authenticate(String token) throws UnauthorizedException {
                    throw new UnauthorizedException();
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/latest")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            
            assertAll(
                () -> assertEquals(403, response.getStatus()),
                () -> assertEquals("Unauthorized", response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void notAuthorized() throws ApiException {
            setAuthManager(new PermissiveAuthManager() {
                @Override
                public void checkReplayAllowed(String user, SubmissionTarget target) throws UnauthorizedException {
                    if (user.equals(GENERATED_USERNAME)) {
                        throw new UnauthorizedException();
                    }
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/latest")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            
            assertAll(
                () -> assertEquals(403, response.getStatus()),
                () -> assertEquals("Unauthorized", response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void nonExistingTargetNotFound() {
            setStorage(new EmptyStorage() {
                
                @Override
                public List<Version> getVersions(SubmissionTarget target)
                        throws NoSuchTargetException, StorageException {
                    throw new NoSuchTargetException(target);
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/latest")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            
            assertAll(
                () -> assertEquals(404, response.getStatus()),
                () -> assertEquals("The group Group01 for assignment Homework01 in course foo-wise2122 does not exist",
                        response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void storageExceptionInternalServerError() {
            setStorage(new EmptyStorage() {
                @Override
                public List<Version> getVersions(SubmissionTarget target)
                        throws NoSuchTargetException, StorageException {
                    throw new StorageException("mock");
                }
            });
            startServer();
    
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/latest")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            
            assertAll(
                () -> assertEquals(500, response.getStatus()),
                () -> assertEquals("Unexpected storage exception: mock", response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void noVersions() {
            startServer();
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/latest")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            
            assertAll(
                () -> assertEquals(404, response.getStatus()),
                () -> assertEquals(
                        "No versions have been submitted for group Group01 in assignment Homework01 in course foo-wise2122",
                        response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void singleVersionWithDirectory() {
            setStorage(new EmptyStorage() {
                @Override
                public List<Version> getVersions(SubmissionTarget target)
                        throws NoSuchTargetException, StorageException {
                    return Arrays.asList(new Version("student123", Instant.now()));
                }
                
                @Override
                public Submission getSubmission(SubmissionTarget target, Version version)
                        throws NoSuchTargetException, StorageException {
                    
                    if (!version.getAuthor().equals("student123")) {
                        throw new StorageException();
                    }
                    
                    SubmissionBuilder builder = new SubmissionBuilder(version.getAuthor());
                    builder.addUtf8File(Path.of("dir/test.txt"), "Some content.\n");
                    
                    return builder.build();
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/latest")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            
            @SuppressWarnings("unchecked")
            List<Map<?, ?>> files = response.readEntity(List.class);
            
            assertAll(
                () -> assertEquals(200, response.getStatus()),
                () -> assertEquals(1, files.size()),
                () -> assertEquals("dir/test.txt", files.get(0).get("path")),
                () -> assertEquals(
                        Base64.getEncoder().encodeToString("Some content.\n".getBytes(StandardCharsets.UTF_8)),
                        files.get(0).get("content"))
            );
        }
        
        @Test
        public void multipleVersions() {
            setStorage(new EmptyStorage() {
                @Override
                public List<Version> getVersions(SubmissionTarget target)
                        throws NoSuchTargetException, StorageException {
                    return Arrays.asList(
                            new Version("student123", Instant.now()),
                            new Version("student321", Instant.now().minus(5, ChronoUnit.SECONDS))
                    );
                }
                
                @Override
                public Submission getSubmission(SubmissionTarget target, Version version)
                        throws NoSuchTargetException, StorageException {
                    
                    SubmissionBuilder builder = new SubmissionBuilder(version.getAuthor());
                    builder.addUtf8File(Path.of("author.txt"), version.getAuthor());
                    return builder.build();
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/latest")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            
            @SuppressWarnings("unchecked")
            List<Map<?, ?>> files = response.readEntity(List.class);
            
            assertAll(
                () -> assertEquals(200, response.getStatus()),
                () -> assertEquals(1, files.size()),
                () -> assertEquals("author.txt", files.get(0).get("path")),
                () -> assertEquals(
                        Base64.getEncoder().encodeToString("student123".getBytes(StandardCharsets.UTF_8)),
                        files.get(0).get("content"))
            );
        }
        
    }
    
    @Nested
    public class GetVersion {
        
        @Test
        public void nonExistingVersionNotFound() {
            Instant t1 = Instant.ofEpochSecond(1634831371L);
            Instant t2 = Instant.ofEpochSecond(1634606632L);
            
            setStorage(new EmptyStorage() {
                @Override
                public List<Version> getVersions(SubmissionTarget target)
                        throws NoSuchTargetException, StorageException {
                    return Arrays.asList(
                        new Version("tommy", t1),
                        new Version("max", t2)
                    );
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/123456")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            
            assertAll(
                () -> assertEquals(404, response.getStatus()),
                () -> assertEquals(
                        "Selected version not found for group Group01 in assignment Homework01 in course foo-wise2122",
                        response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void selectFirst() {
            Instant t1 = Instant.ofEpochSecond(1634831371L);
            Instant t2 = Instant.ofEpochSecond(1634606632L);
            
            setStorage(new EmptyStorage() {
                @Override
                public List<Version> getVersions(SubmissionTarget target)
                        throws NoSuchTargetException, StorageException {
                    return Arrays.asList(
                            new Version("tommy", t1),
                            new Version("max", t2)
                    );
                }
                
                @Override
                public Submission getSubmission(SubmissionTarget target, Version version)
                        throws NoSuchTargetException, StorageException {
                    SubmissionBuilder builder = new SubmissionBuilder(version.getAuthor());
                    builder.addUtf8File(Path.of("author.txt"), version.getAuthor());
                    return builder.build();
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/1634831371")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            
            @SuppressWarnings("unchecked")
            List<Map<?, ?>> files = response.readEntity(List.class);
            
            assertAll(
                () -> assertEquals(200, response.getStatus()),
                () -> assertEquals(1, files.size()),
                () -> assertEquals("author.txt", files.get(0).get("path")),
                () -> assertEquals(
                        Base64.getEncoder().encodeToString("tommy".getBytes(StandardCharsets.UTF_8)),
                        files.get(0).get("content"))
            );
        }
        
        @Test
        public void selectSecond() {
            Instant t1 = Instant.ofEpochSecond(1634831371L);
            Instant t2 = Instant.ofEpochSecond(1634606632L);
            
            setStorage(new EmptyStorage() {
                @Override
                public List<Version> getVersions(SubmissionTarget target)
                        throws NoSuchTargetException, StorageException {
                    return Arrays.asList(
                            new Version("tommy", t1),
                            new Version("max", t2)
                        );
                }
                
                @Override
                public Submission getSubmission(SubmissionTarget target, Version version)
                        throws NoSuchTargetException, StorageException {
                    SubmissionBuilder builder = new SubmissionBuilder(version.getAuthor());
                    builder.addUtf8File(Path.of("author.txt"), version.getAuthor());
                    return builder.build();
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/1634606632")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            
            @SuppressWarnings("unchecked")
            List<Map<?, ?>> files = response.readEntity(List.class);
            
            assertAll(
                () -> assertEquals(200, response.getStatus()),
                () -> assertEquals(1, files.size()),
                () -> assertEquals("author.txt", files.get(0).get("path")),
                () -> assertEquals(
                        Base64.getEncoder().encodeToString("max".getBytes(StandardCharsets.UTF_8)),
                        files.get(0).get("content"))
            );
        }
        
    }
    
}
