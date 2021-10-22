package net.ssehub.teaching.exercise_submitter.server.storage;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class SubmissionTest {

    private Path temporaryDirectory;
    
    @Test
    public void emptySubmission() {
        Submission submission = new Submission(Collections.emptyMap());
        
        assertAll(
            () -> assertEquals(0, submission.getNumFiles()),
            () -> assertEquals(Collections.emptySet(), submission.getFilepaths()),
            () -> assertFalse(submission.containsFile(Path.of("test.txt")))
        );
    }
    
    @Test
    public void singleFile() {
        Map<Path, String> files = new HashMap<>();
        files.put(Path.of("test.txt"), "some content\n");
        Submission submission = new Submission(files);
        
        assertAll(
            () -> assertEquals(1, submission.getNumFiles()),
            () -> assertEquals(new HashSet<>(Arrays.asList(Path.of("test.txt"))), submission.getFilepaths()),
            () -> assertTrue(submission.containsFile(Path.of("test.txt"))),
            () -> assertEquals("some content\n", submission.getFileContent(Path.of("test.txt")))
        );
    }
    
    @Test
    public void multipleFile() {
        Map<Path, String> files = new HashMap<>();
        files.put(Path.of("test.txt"), "some content\n");
        files.put(Path.of("dir/other.txt"), "other content\n");
        Submission submission = new Submission(files);
        
        assertAll(
            () -> assertEquals(2, submission.getNumFiles()),
            () -> assertEquals(new HashSet<>(Arrays.asList(Path.of("test.txt"), Path.of("dir/other.txt"))),
                    submission.getFilepaths()),
            () -> assertTrue(submission.containsFile(Path.of("test.txt"))),
            () -> assertTrue(submission.containsFile(Path.of("dir/other.txt"))),
            () -> assertFalse(submission.containsFile(Path.of("other.txt"))),
            () -> assertEquals("some content\n", submission.getFileContent(Path.of("test.txt"))),
            () -> assertEquals("other content\n", submission.getFileContent(Path.of("dir/other.txt")))
        );
    }
    
    @Test
    public void getFileContentOnNonExistingFileThrows() {
        Submission submission = new Submission(Collections.emptyMap());
        
        NoSuchElementException e = assertThrows(NoSuchElementException.class,
            () -> submission.getFileContent(Path.of("test.txt")));
        assertEquals("File " + Path.of("test.txt") + " does not exist in this submission", e.getMessage());
    }
    
    @Test
    public void writeToNonExistingDirectoryThrows() {
        Submission submission = new Submission(Collections.emptyMap());
        
        IOException e = assertThrows(IOException.class, () -> submission.writeToDirectory(Path.of("doesnt_exist")));
        assertEquals("doesnt_exist is not a directory", e.getMessage());
    }
    
    @Test
    public void writeEmptySubmission() throws IOException {
        Submission submission = new Submission(Collections.emptyMap());
        
        temporaryDirectory = Files.createTempDirectory("SubmissionTest.writeEmptySubmission");
        
        submission.writeToDirectory(temporaryDirectory);
        
        assertEquals(0, Files.list(temporaryDirectory).count());
    }
    
    @Test
    public void writeSingleFileSubmission() throws IOException {
        Map<Path, String> files = new HashMap<>();
        files.put(Path.of("test.txt"), "some content\n");
        Submission submission = new Submission(files);
        
        temporaryDirectory = Files.createTempDirectory("SubmissionTest.writeSingleFileSubmission");
        
        submission.writeToDirectory(temporaryDirectory);
        
        assertAll(
            () -> assertEquals(Arrays.asList(Path.of("test.txt")),
                        Files.list(temporaryDirectory)
                        .map(p -> temporaryDirectory.relativize(p))
                        .collect(Collectors.toList())),
            () -> assertEquals("some content\n",
                    Files.readString(temporaryDirectory.resolve("test.txt"), StandardCharsets.UTF_8))
        );
    }
    
    @Test
    public void writeMultipleFileSubmission() throws IOException {
        Map<Path, String> files = new HashMap<>();
        files.put(Path.of("test.txt"), "some content\n");
        files.put(Path.of("other.txt"), "other content\n");
        Submission submission = new Submission(files);
        
        temporaryDirectory = Files.createTempDirectory("SubmissionTest.writeMultipleFileSubmission");
        
        submission.writeToDirectory(temporaryDirectory);
        
        assertAll(
            () -> assertEquals(new HashSet<>(Arrays.asList(Path.of("test.txt"), Path.of("other.txt"))),
                    Files.list(temporaryDirectory)
                    .map(p -> temporaryDirectory.relativize(p))
                    .collect(Collectors.toSet())),
            () -> assertEquals("some content\n",
                    Files.readString(temporaryDirectory.resolve("test.txt"), StandardCharsets.UTF_8)),
            () -> assertEquals("other content\n",
                    Files.readString(temporaryDirectory.resolve("other.txt"), StandardCharsets.UTF_8))
        );
    }
    
    @Test
    public void writeMultipleFilesInDirectories() throws IOException {
        Map<Path, String> files = new HashMap<>();
        files.put(Path.of("dir1/test.txt"), "some content\n");
        files.put(Path.of("dir1/other.txt"), "other content\n");
        files.put(Path.of("dir2/subdir/other.txt"), "even different content\n");
        Submission submission = new Submission(files);
        
        temporaryDirectory = Files.createTempDirectory("SubmissionTest.writeMultipleFilesInDirectories");
        
        submission.writeToDirectory(temporaryDirectory);
        
        assertAll(
            () -> assertEquals(new HashSet<>(Arrays.asList(
                    Path.of("dir1/test.txt"), Path.of("dir1/other.txt"), Path.of("dir2/subdir/other.txt"))),
                    
                    Files.walk(temporaryDirectory)
                    .filter(p -> Files.isRegularFile(p))
                    .map(p -> temporaryDirectory.relativize(p))
                    .collect(Collectors.toSet())),
            
            () -> assertEquals("some content\n",
                    Files.readString(temporaryDirectory.resolve("dir1/test.txt"), StandardCharsets.UTF_8)),
            () -> assertEquals("other content\n",
                    Files.readString(temporaryDirectory.resolve("dir1/other.txt"), StandardCharsets.UTF_8)),
            () -> assertEquals("even different content\n",
                    Files.readString(temporaryDirectory.resolve("dir2/subdir/other.txt"), StandardCharsets.UTF_8))
        );
    }
    
    @Test
    public void writeOverwritesExistingFile() throws IOException {
        Map<Path, String> files = new HashMap<>();
        files.put(Path.of("test.txt"), "some content\n");
        Submission submission = new Submission(files);
        
        temporaryDirectory = Files.createTempDirectory("SubmissionTest.writeSingleFileSubmission");
        Files.writeString(temporaryDirectory.resolve("test.txt"), "previous content\n", StandardCharsets.UTF_8);
        
        submission.writeToDirectory(temporaryDirectory);
        
        assertAll(
            () -> assertEquals(Arrays.asList(Path.of("test.txt")),
                        Files.list(temporaryDirectory)
                        .map(p -> temporaryDirectory.relativize(p))
                        .collect(Collectors.toList())),
            () -> assertEquals("some content\n",
                    Files.readString(temporaryDirectory.resolve("test.txt"), StandardCharsets.UTF_8))
        );
    }
    
    @AfterEach
    public void cleanTemporaryDirectory() throws IOException {
        if (temporaryDirectory != null) {
            try (Stream<Path> walk = Files.walk(temporaryDirectory)) {
                walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        }
    }
    
}
