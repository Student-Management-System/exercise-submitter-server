package net.ssehub.teaching.exercise_submitter.server.submission.checks;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

public class SrcFolderCheckTest {
    
    private static final Path TESTDATA = Path.of("src/test/resources/SrcFolderCheckTest");
    
    @Test
    public void noSrcFolderSucceeds() {
        Path directory = TESTDATA.resolve("noSrcFolder");
        assertTrue(Files.isDirectory(directory), "Precondition: testdata exist");
        
        SrcFolderCheck check = new SrcFolderCheck();
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertTrue(success),
            () -> assertEquals(Collections.EMPTY_LIST, check.getResultMessages())
        );
    }
    
    @Test
    public void srcFolderWithNoJavaFilesSucceeds() {
        Path directory = TESTDATA.resolve("srcFolderWithoutJavaFiles");
        assertTrue(Files.isDirectory(directory), "Precondition: testdata exist");
        
        SrcFolderCheck check = new SrcFolderCheck();
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertTrue(success),
            () -> assertEquals(Collections.EMPTY_LIST, check.getResultMessages())
        );
    }
    
    @Test
    public void srcFolderWithJavaFilesFails() {
        Path directory = TESTDATA.resolve("withSrcFolder");
        assertTrue(Files.isDirectory(directory), "Precondition: testdata exist");
        
        SrcFolderCheck check = new SrcFolderCheck();
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertFalse(success),
            () -> assertEquals(Arrays.asList(new ResultMessage("src-folder", MessageType.ERROR,
                    "Submission contains a src folder with Java source files")), check.getResultMessages())
        );
    }

}
