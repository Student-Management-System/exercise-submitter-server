package net.ssehub.teaching.exercise_submitter.server.submission.checks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

/**
 * Checks that the submission does not contain an <code>src</code> folder with Java files. If this folder is present,
 * the Eclipse instance of the student is probably set up with the wrong configuration. This causes compilation
 * problems that are not caught by the {@link JavacCheck}. 
 * 
 * @author Adam
 */
public class SrcFolderCheck extends Check {
    
    public static final String CHECK_NAME = "src-folder";
    
    private static final Logger LOGGER = Logger.getLogger(SrcFolderCheck.class.getName());

    @Override
    public boolean run(Path submissionDirectory) {
        
        Path srcFolder = submissionDirectory.resolve("src");
        boolean hasSrcFolder = Files.isDirectory(srcFolder);
        boolean srcContainsJavaFiles = false;
        if (hasSrcFolder) {
            try {
                srcContainsJavaFiles = Files.walk(srcFolder)
                        .filter(file -> Files.isRegularFile(file))
                        .filter(file -> file.getFileName().toString().endsWith(".java"))
                        .findAny()
                        .isPresent();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Exception while checking for Java files in src folder", e);
                srcContainsJavaFiles = true; // be conservative here; assume a src folder is bad
                
                addResultMessage(new ResultMessage(CHECK_NAME, MessageType.ERROR,
                        "An internal error occurred while checking the src folder"));
            }
        }
        
        if (srcContainsJavaFiles) {
            addResultMessage(new ResultMessage(CHECK_NAME, MessageType.ERROR,
                    "Submission contains a src folder with Java source files"));
        }
        
        return !srcContainsJavaFiles;
    }

}
