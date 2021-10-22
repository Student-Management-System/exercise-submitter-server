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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.ssehub.teaching.exercise_submitter.server.auth.PermissiveAuthManager;
import net.ssehub.teaching.exercise_submitter.server.storage.EmptyStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchTargetException;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.Submission;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionBuilder;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.storage.Version;
import net.ssehub.teaching.exercise_submitter.server.submission.NoChecksSubmissionManager;
import net.ssehub.teaching.exercise_submitter.server.submission.UnauthorizedException;

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
                    .post(Entity.entity(Map.of(), MediaType.APPLICATION_JSON));
            
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
                    .post(Entity.entity(Map.of("../test.txt", "some content\n"), MediaType.APPLICATION_JSON));
            
            assertAll(
                () -> assertEquals(400, response.getStatus()),
                () -> assertEquals("Invalid filepath: .. is not allowed in submission paths",
                        response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void notAuthenticated() {
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
                    .post(Entity.entity(Map.of("test.txt", "some content\n"), MediaType.APPLICATION_JSON));
            
            assertAll(
                () -> assertEquals(403, response.getStatus()),
                () -> assertEquals("Unauthorized", response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void notAuthorized() {
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
                    .post(Entity.entity(Map.of("test.txt", "some content\n"), MediaType.APPLICATION_JSON));
            
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
                    .post(Entity.entity(Map.of("test.txt", "some content\n"), MediaType.APPLICATION_JSON));
            
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
                    .post(Entity.entity(Map.of("test.txt", "some content\n"), MediaType.APPLICATION_JSON));
            
            assertAll(
                () -> assertEquals(500, response.getStatus()),
                () -> assertEquals("Unexpected storage exception: mock", response.getStatusInfo().getReasonPhrase())
            );
        }
        
        @Test
        public void validSubmissionStored() {
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
                    .post(Entity.entity(Map.of("test.txt", "some content\n"), MediaType.APPLICATION_JSON));
            
            assertAll(
                () -> assertEquals(201, response.getStatus()),
                () -> assertEquals(1, result.get().getNumFiles()),
                () -> assertEquals("some content\n", result.get().getFileContent(Path.of("test.txt")))
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
        public void notAuthenticated() {
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
        public void notAuthorized() {
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
            // unix timestamp 1634831371
            LocalDateTime t1 =  LocalDateTime.ofInstant(Instant.parse("2021-10-21T15:49:31Z"), ZoneId.systemDefault());
            // unix timestamp 1634606632
            LocalDateTime t2 =  LocalDateTime.ofInstant(Instant.parse("2021-10-19T01:23:52Z"), ZoneId.systemDefault());
            
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
        public void notAuthenticated() {
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
        public void notAuthorized() {
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
                    return Arrays.asList(new Version("student123", LocalDateTime.now()));
                }
                
                @Override
                public Submission getSubmission(SubmissionTarget target, Version version)
                        throws NoSuchTargetException, StorageException {
                    
                    if (!version.getAuthor().equals("student123")) {
                        throw new StorageException();
                    }
                    
                    SubmissionBuilder builder = new SubmissionBuilder(version.getAuthor());
                    builder.addFile(Path.of("dir/test.txt"), "Some content.\n");
                    
                    return builder.build();
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/latest")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            Map<?, ?> result = response.readEntity(Map.class);
            
            assertAll(
                () -> assertEquals(200, response.getStatus()),
                () -> assertEquals(Map.of("dir/test.txt", "Some content.\n"), result)
            );
        }
        
        @Test
        public void multipleVersions() {
            setStorage(new EmptyStorage() {
                @Override
                public List<Version> getVersions(SubmissionTarget target)
                        throws NoSuchTargetException, StorageException {
                    return Arrays.asList(
                            new Version("student123", LocalDateTime.now()),
                            new Version("student321", LocalDateTime.now())
                    );
                }
                
                @Override
                public Submission getSubmission(SubmissionTarget target, Version version)
                        throws NoSuchTargetException, StorageException {
                    
                    SubmissionBuilder builder = new SubmissionBuilder(version.getAuthor());
                    builder.addFile(Path.of("author.txt"), version.getAuthor());
                    return builder.build();
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/latest")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            Map<?, ?> result = response.readEntity(Map.class);
            
            assertAll(
                () -> assertEquals(200, response.getStatus()),
                () -> assertEquals("student123", result.get("author.txt"))
            );
        }
        
    }
    
    @Nested
    public class GetVersion {
        
        @Test
        public void nonExistingVersionNotFound() {
            // unix timestamp 1634831371
            LocalDateTime t1 =  LocalDateTime.ofInstant(Instant.parse("2021-10-21T15:49:31Z"), ZoneId.systemDefault());
            // unix timestamp 1634606632
            LocalDateTime t2 =  LocalDateTime.ofInstant(Instant.parse("2021-10-19T01:23:52Z"), ZoneId.systemDefault());
            
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
            // unix timestamp 1634831371
            LocalDateTime t1 =  LocalDateTime.ofInstant(Instant.parse("2021-10-21T15:49:31Z"), ZoneId.systemDefault());
            // unix timestamp 1634606632
            LocalDateTime t2 =  LocalDateTime.ofInstant(Instant.parse("2021-10-19T01:23:52Z"), ZoneId.systemDefault());
            
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
                    builder.addFile(Path.of("author.txt"), version.getAuthor());
                    return builder.build();
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/1634831371")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            Map<?, ?> result = response.readEntity(Map.class);
            
            assertAll(
                () -> assertEquals(200, response.getStatus()),
                () -> assertEquals("tommy", result.get("author.txt"))
            );
        }
        
        @Test
        public void selectSecond() {
            // unix timestamp 1634831371
            LocalDateTime t1 =  LocalDateTime.ofInstant(Instant.parse("2021-10-21T15:49:31Z"), ZoneId.systemDefault());
            // unix timestamp 1634606632
            LocalDateTime t2 =  LocalDateTime.ofInstant(Instant.parse("2021-10-19T01:23:52Z"), ZoneId.systemDefault());
            
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
                    builder.addFile(Path.of("author.txt"), version.getAuthor());
                    return builder.build();
                }
            });
            startServer();
            
            Response response = target.path("/submission/foo-wise2122/Homework01/Group01/1634606632")
                    .request()
                    .header("Authorization", JWT_TOKEN)
                    .get();
            Map<?, ?> result = response.readEntity(Map.class);
            
            assertAll(
                () -> assertEquals(200, response.getStatus()),
                () -> assertEquals("max", result.get("author.txt"))
            );
        }
        
    }
    
}
