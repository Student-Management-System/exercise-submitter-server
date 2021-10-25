package net.ssehub.teaching.exercise_submitter.server.storage;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

public class SubmissionBuilderTest {
    
    @Test
    public void author() {
        SubmissionBuilder builder = new SubmissionBuilder("some author");
        assertEquals("some author", builder.build().getAuthor());
    }

    @Test
    public void addAbsolutePathThrows() {
        SubmissionBuilder builder = new SubmissionBuilder("author");
        
        Path absolutePath = Path.of("somethig").toAbsolutePath();
        
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> builder.addFile(absolutePath, "some content\n".getBytes(StandardCharsets.UTF_8)));
        
        assertEquals(absolutePath + " is absolute", e.getMessage());
    }
    
    @Test
    public void doubleBuildThrows() {
        SubmissionBuilder builder = new SubmissionBuilder("author");
        builder.build();
        
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> builder.build());
        assertEquals("build() was already called", e.getMessage());
    }
    
    @Test
    public void addFileAfterBuildThrows() {
        SubmissionBuilder builder = new SubmissionBuilder("author");
        builder.build();
        
        IllegalStateException e = assertThrows(IllegalStateException.class,
            () -> builder.addFile(Path.of(""), "content\n".getBytes(StandardCharsets.UTF_8)));
        assertEquals("build() was already called", e.getMessage());
    }
    
    @Test
    public void addFileAddsFiles() {
        SubmissionBuilder builder = new SubmissionBuilder("author");
        
        builder.addFile(Path.of("test.txt"), "first content\n".getBytes(StandardCharsets.UTF_8));
        builder.addFile(Path.of("dir/other.txt"), "second content\n".getBytes(StandardCharsets.UTF_8));
        
        Submission submission = builder.build();
        assertAll(
            () -> assertEquals(2, submission.getNumFiles()),
            () -> assertEquals(new HashSet<>(Arrays.asList(Path.of("test.txt"), Path.of("dir/other.txt"))),
                    submission.getFilepaths()),
            () -> assertArrayEquals("first content\n".getBytes(StandardCharsets.UTF_8),
                    submission.getFileContent(Path.of("test.txt"))),
            () -> assertArrayEquals("second content\n".getBytes(StandardCharsets.UTF_8),
                    submission.getFileContent(Path.of("dir/other.txt")))
        );
    }
    
    @Test
    public void filepathContainsDotDot() {
        SubmissionBuilder builder = new SubmissionBuilder("author");
        
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> builder.addFile(Path.of("../test.txt"), "something\n".getBytes(StandardCharsets.UTF_8)));
        assertEquals(".. is not allowed in submission paths", e.getMessage());
    }
    
    @Test
    public void addUtf8File() {
        SubmissionBuilder builder = new SubmissionBuilder("author");
        
        builder.addUtf8File(Path.of("test.txt"), "söme cöntent");
        
        Submission submission = builder.build();
        
        assertAll(
            () -> assertEquals(1, submission.getNumFiles()),
            () -> assertEquals(new HashSet<>(Arrays.asList(Path.of("test.txt"))), submission.getFilepaths()),
            () -> assertArrayEquals("söme cöntent".getBytes(StandardCharsets.UTF_8),
                    submission.getFileContent(Path.of("test.txt")))
        );
    }
    
}
