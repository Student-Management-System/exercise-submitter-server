package net.ssehub.teaching.exercise_submitter.server.submission;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.ssehub.teaching.exercise_submitter.server.rest.dto.CheckMessageDto;
import net.ssehub.teaching.exercise_submitter.server.rest.dto.SubmissionResultDto;
import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchTargetException;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.Submission;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.Check;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

/**
 * This class orchestrates a complete submission process. This should be the entry point for starting a submission.
 * 
 * @author Adam
 *
 */
public class SubmissionManager {
    
    private static final Logger LOGGER = Logger.getLogger(SubmissionManager.class.getName());
    
    private ISubmissionStorage storage;
    
    private StuMgmtView stuMgmtView;
    
    private List<Check> rejectingChecks;
    
    private List<Check> nonRejectingChecks;
    
    /**
     * Creates a new {@link SubmissionManager}.
     * 
     * @param storage The storage component to use.
     * @param stuMgmtView The view on the student management system to inform about check results. 
     */
    public SubmissionManager(ISubmissionStorage storage, StuMgmtView stuMgmtView) {
        this.storage = storage;
        this.stuMgmtView = stuMgmtView;
        
        this.rejectingChecks = new LinkedList<>();
        this.nonRejectingChecks = new LinkedList<>();
    }
    
    /**
     * Adds a {@link Check} that will reject submissions if it fails.
     * 
     * @param check The check to perform on submissions.
     */
    public void addRejectingCheck(Check check) {
        this.rejectingChecks.add(check);
    }
    
    /**
     * Adds a {@link Check} that will <b>not</b> reject submissions if it fails. It's {@link ResultMessage}s will
     * appear in the response, though.
     * 
     * @param check The check to perform on submissions.
     */
    public void addNonRejectingCheck(Check check) {
        this.nonRejectingChecks.add(check);
    }
    
    /**
     * Executes a full submission.
     * <p>
     * This class runs the necessary {@link Check}s.
     * 
     * @param target The assignment and group to submit to.
     * @param submission The submission to add.
     * 
     * @return The result of the submission, including the messages created by the {@link Check}s.
     * 
     * @throws NoSuchTargetException If the given target does not exist.
     * @throws StorageException If an exception occurred in the storage backend.
     */
    public SubmissionResultDto submit(SubmissionTarget target, Submission submission)
            throws NoSuchTargetException, StorageException {
    
        List<ResultMessage> checkMessages = new LinkedList<>();
        boolean reject = false;
        
        Path temporaryDirectory = null;
        try {
            temporaryDirectory = Files.createTempDirectory("exercise-submission");
            submission.writeToDirectory(temporaryDirectory);
            
            for (Check check : rejectingChecks) {
                boolean passed = check.run(temporaryDirectory.toFile());
                checkMessages.addAll(check.getResultMessages());
                
                if (!passed) {
                    reject = true;
                    break;
                }
            }
            
            if (!reject) {
                for (Check check : nonRejectingChecks) {
                    check.run(temporaryDirectory.toFile());
                    checkMessages.addAll(check.getResultMessages());
                }
                
                storage.submitNewVersion(target, submission);
            }
            
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to create temporary submission directory", e);
            reject = true;
            checkMessages.add(new ResultMessage("hook", MessageType.ERROR, "An internal error occurred"));
            
        } finally {
            if (temporaryDirectory != null) {
                deleteDirectory(temporaryDirectory);
            }
        }
        
        Collections.sort(checkMessages);
        
        SubmissionResultDto result = new SubmissionResultDto();
        result.setAccepted(!reject);
        result.setMessages(checkMessages.stream()
                .map(CheckMessageDto::new)
                .collect(Collectors.toList()));
        
        LOGGER.info(() -> "Submission to " + target + " " + (result.getAccepted() ? "accepted" : "rejected")
                + "; messages: " + result.getMessages());
        
        if (!reject && !nonRejectingChecks.isEmpty()) {
            stuMgmtView.sendSubmissionResult(target, checkMessages);
        }
        
        return result;
    }

    /**
     * Deletes the given directory and all content recursively.
     * 
     * @param directory The directory to delete.
     */
    private void deleteDirectory(Path directory) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to delete temporary submission directory", e);
        }
    }
    
}
