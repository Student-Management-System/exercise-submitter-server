package net.ssehub.teaching.exercise_submitter.server.submission;

import net.ssehub.teaching.exercise_submitter.server.checks.Check;
import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchTargetException;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.Submission;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;

/**
 * This class orchestrates a complete submission process. This should be the entry point for starting a submission.
 * 
 * @author Adam
 *
 */
public class SubmissionManager {
    
    private ISubmissionStorage storage;
    
    /**
     * Creates a new {@link SubmissionManager}.
     * 
     * @param storage The storage component to use.
     */
    public SubmissionManager(ISubmissionStorage storage) {
        this.storage = storage;
    }
    
    /**
     * Executes a full submission. TODO: add a return type.
     * <p>
     * This class runs the necessary {@link Check}s.
     * 
     * @param target The assignment and group to submit to.
     * @param submission The submission to add.
     * 
     * @throws NoSuchAssignmentException If no assignment with the given name exists.
     * @throws NoSuchGroupException If no group with the given name exists.
     * @throws StorageException If an exception occurred in the storage backend.
     */
    public void submit(SubmissionTarget target, Submission submission)
            throws NoSuchTargetException, StorageException {
    
        storage.submitNewVersion(target, submission);
    }
    
}
