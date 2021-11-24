package net.ssehub.teaching.exercise_submitter.server;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.ssehub.studentmgmt.docker.StuMgmtDocker;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.AssignmentState;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.Collaboration;
import net.ssehub.teaching.exercise_submitter.server.rest.ExerciseSubmitterServer;
import net.ssehub.teaching.exercise_submitter.server.rest.dto.FileDto;
import net.ssehub.teaching.exercise_submitter.server.rest.dto.SubmissionResultDto;
import net.ssehub.teaching.exercise_submitter.server.rest.routes.AbstractRestTest;

public class ScenarioIT {
    
    private static final Path TESTDATA = Path.of("src", "test", "resources", "ScenarioIT");

    private static StuMgmtDocker docker;
    
    private static ExerciseSubmitterServer server;
    
    private static WebTarget target;
    
    private static Path storagePath;
    
    @BeforeAll
    public static void startServer() throws IOException {
        docker = new StuMgmtDocker();
        
        docker.createUser("teacher", "abcdefgh");
        docker.createUser("submission-system", "123456");
        docker.createUser("student1", "abcdefgh");
        docker.createUser("student2", "abcdefgh");
        docker.createUser("student3", "abcdefgh");
        
        String course = docker.createCourse("java", "wise2122", "Lab Course Java", "teacher", "submission-system");
        docker.enrollStudent(course, "student1");
        docker.enrollStudent(course, "student2");
        docker.enrollStudent(course, "student3");
        
        docker.createGroup(course, "TheOdds", "student1", "student3");
        docker.createGroup(course, "TheEvens", "student2");
        
        // used in singleFileSubmissionAndReplay()
        docker.createAssignment(course, "Homework01", AssignmentState.SUBMISSION, Collaboration.GROUP);
        // used in submissionWithCompilationErrorAndReplay()
        docker.createAssignment(course, "Testat", AssignmentState.SUBMISSION, Collaboration.SINGLE);
        // used in submissionWithBinaryFile()
        docker.createAssignment(course, "Homework02", AssignmentState.SUBMISSION, Collaboration.GROUP);
        
        
        int port = AbstractRestTest.generateRandomPort();
        Client client = ClientBuilder.newClient();
        target = client.target("http://localhost:" + port + "/");
        
        storagePath = Files.createTempDirectory("exercise-submitter-server-scenario-it");
        
        server = ExerciseSubmitterServer.startDefaultServer(port, storagePath.toString(),
                docker.getAuthUrl(), docker.getStuMgmtUrl(), "submission-system", "123456",
                Optional.empty(), Optional.empty());
    }
    
    @AfterAll
    public static void stopServer() {
        server.stop();
        docker.close();
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void singleFileSubmissionAndReplay() throws IOException {
        byte[] fileContent = Files.readAllBytes(TESTDATA.resolve("singleFile/Main.java"));
        
        // student1 submits
        
        Response submissionResponse = target.path("/submission/java-wise2122/Homework01/TheOdds")
                .request()
                .header("Authorization", "Bearer " + docker.getAuthToken("student1"))
                .post(Entity.entity(Arrays.asList(new FileDto("Main.java", fileContent)), MediaType.APPLICATION_JSON));
        
        
        SubmissionResultDto submissionResult = submissionResponse.readEntity(SubmissionResultDto.class);
        
        Path groupDir = storagePath.resolve(Path.of("java-wise2122", "Homework01", "TheOdds"));
        
        assertAll(
            () -> assertEquals(201, submissionResponse.getStatus()),
            () -> assertTrue(submissionResult.getAccepted()),
//            () -> assertEquals(Collections.emptyList(), submissionResult.getMessages()), // TODO: checkstyle.xml not found
            () -> assertEquals(1L, Files.list(groupDir).count()),
            () -> assertArrayEquals(fileContent,
                    Files.readAllBytes(Files.list(groupDir).findFirst().get().resolve("Main.java")))
        );
        
        // student3 replays
        
        Response versionsResponse = target.path("/submission/java-wise2122/Homework01/TheOdds/versions")
                .request()
                .header("Authorization", "Bearer " + docker.getAuthToken("student3"))
                .get();
        
        List<?> versionsResult = versionsResponse.readEntity(List.class);
        long timestamp = ((Map<String, BigDecimal>) versionsResult.get(0)).get("timestamp").longValue();
        long diffToNow = Instant.now().minus(Instant.ofEpochSecond(timestamp).getEpochSecond(), ChronoUnit.SECONDS)
                    .getEpochSecond();
        
        assertAll(
            () -> assertEquals(200, versionsResponse.getStatus()),
            () -> assertEquals(1, versionsResult.size()),
            () -> assertEquals("student1", ((Map<String, Object>) versionsResult.get(0)).get("author")),
            () -> assertTrue(diffToNow < 60 && diffToNow >= 0) // check that timestamp is reasonable (within the last minute)
        );
        
        Response replayResponse = target.path("/submission/java-wise2122/Homework01/TheOdds/" + timestamp)
                .request()
                .header("Authorization", "Bearer " + docker.getAuthToken("student3"))
                .get();
        
        List<Map<?, ?>> replayResult = replayResponse.readEntity(List.class);
        
        assertAll(
            () -> assertEquals(200, replayResponse.getStatus()),
            () -> assertEquals(1, replayResult.size()),
            () -> assertEquals("Main.java", replayResult.get(0).get("path")),
            () -> assertEquals(Base64.getEncoder().encodeToString(fileContent),
                    replayResult.get(0).get("content"))
        );
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void submissionWithCompilationErrorAndReplay() throws IOException {
        byte[] fileContent = Files.readAllBytes(TESTDATA.resolve("compilationError/Main.java"));
        
        Response submissionResponse = target.path("/submission/java-wise2122/Testat/student1")
                .request()
                .header("Authorization", "Bearer " + docker.getAuthToken("student1"))
                .post(Entity.entity(Arrays.asList(new FileDto("Main.java", fileContent)), MediaType.APPLICATION_JSON));
        
        
        SubmissionResultDto submissionResult = submissionResponse.readEntity(SubmissionResultDto.class);
        
        Path groupDir = storagePath.resolve(Path.of("java-wise2122", "Testat", "student1"));
        
        assertAll(
            () -> assertEquals(201, submissionResponse.getStatus()),
            () -> assertTrue(submissionResult.getAccepted()),
            // TODO: first checkmessage is about checkstyle.xml missing
            () -> assertEquals("javac", submissionResult.getMessages().get(1).getCheckName()),
            () -> assertEquals(1L, Files.list(groupDir).count()),
            () -> assertArrayEquals(fileContent,
                        Files.readAllBytes(Files.list(groupDir).findFirst().get().resolve("Main.java")))
            );
        
        // student1 replays
        
        Response versionsResponse = target.path("/submission/java-wise2122/Testat/student1/versions")
                .request()
                .header("Authorization", "Bearer " + docker.getAuthToken("student1"))
                .get();
        
        List<?> versionsResult = versionsResponse.readEntity(List.class);
        
        long timestamp = ((Map<String, BigDecimal>) versionsResult.get(0)).get("timestamp").longValue();
        long diffToNow = Instant.now().minus(Instant.ofEpochSecond(timestamp).getEpochSecond(), ChronoUnit.SECONDS)
                .getEpochSecond();
        
        assertAll(
            () -> assertEquals(200, versionsResponse.getStatus()),
            () -> assertEquals(1, versionsResult.size()),
            () -> assertEquals("student1", ((Map<String, Object>) versionsResult.get(0)).get("author")),
            () -> assertTrue(diffToNow < 60 && diffToNow >= 0) // check that timestamp is reasonable (within the last minute)
        );
        
        
        Response replayResponse = target.path("/submission/java-wise2122/Testat/student1/" + timestamp)
                .request()
                .header("Authorization", "Bearer " + docker.getAuthToken("student1"))
                .get();
        
        List<Map<?, ?>> replayResult = replayResponse.readEntity(List.class);
        
        assertAll(
            () -> assertEquals(200, replayResponse.getStatus()),
            () -> assertEquals(1, replayResult.size()),
            () -> assertEquals("Main.java", replayResult.get(0).get("path")),
            () -> assertEquals(Base64.getEncoder().encodeToString(fileContent),
                    replayResult.get(0).get("content"))
        );
    }
    
    @Test
    public void submissionWithBinaryFile() throws IOException {
        byte[] fileContent = Files.readAllBytes(TESTDATA.resolve("binaryFile/one-pixel.png"));
        
        Response submissionResponse = target.path("/submission/java-wise2122/Homework02/TheEvens")
                .request()
                .header("Authorization", "Bearer " + docker.getAuthToken("student2"))
                .post(Entity.entity(Arrays.asList(new FileDto("one-pixel.png", fileContent)), MediaType.APPLICATION_JSON));
        
        SubmissionResultDto submissionResult = submissionResponse.readEntity(SubmissionResultDto.class);

        Path groupDir = storagePath.resolve(Path.of("java-wise2122", "Homework02", "TheEvens"));
        
        assertAll(
            () -> assertEquals(201, submissionResponse.getStatus()),
            () -> assertTrue(submissionResult.getAccepted()),
            () -> assertEquals(1, submissionResult.getMessages().size()),
            () -> assertEquals("javac", submissionResult.getMessages().get(0).getCheckName()),
            () -> assertEquals("No Java files found", submissionResult.getMessages().get(0).getMessage()),
            () -> assertEquals(1L, Files.list(groupDir).count()),
            () -> assertArrayEquals(fileContent,
                    Files.readAllBytes(Files.list(groupDir).findFirst().get().resolve("one-pixel.png")))
        );
    }
    
}
