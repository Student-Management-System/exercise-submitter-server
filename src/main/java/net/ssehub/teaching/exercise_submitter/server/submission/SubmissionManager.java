package net.ssehub.teaching.exercise_submitter.server.submission;

import java.util.List;

import net.ssehub.teaching.exercise_submitter.server.checks.Check;
import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchAssignmentException;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchGroupException;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchVersionException;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.Submission;
import net.ssehub.teaching.exercise_submitter.server.storage.Version;

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
     * Executes a full submission.
     * <p>
     * This class checks that the author has the proper permissions to add submission and also runs the necessary
     * {@link Check}s.
     * 
     * @param assignmentName The name of the assignment to submit to.
     * @param groupName The name of the group in the assignment to submit to.
     * @param author The author that created this submission.
     * @param submission The submission to add.
     * 
     * @throws NoSuchAssignmentException If no assignment with the given name exists.
     * @throws NoSuchGroupException If no group with the given name exists.
     * @throws StorageException If an exception occurred in the storage backend.
     * @throws UnauthorizedException If the given author is not authorized to submit a new version.
     */
    public void submit(String assignmentName, String groupName, String author, Submission submission)
            throws NoSuchAssignmentException, NoSuchGroupException, StorageException, UnauthorizedException {
    
        storage.submitNewVersion(assignmentName, groupName, author, submission);
    }
    
    /**
     * Retrieves the list of versions for a given group and assignment.
     * 
     * @param assignmentName The name of the assignment.
     * @param groupName The name of the group.
     * @param user The user that tries to access this information.
     *  
     * @return The list of versions that the group has submitted for the given assignment.
     * 
     * @throws NoSuchAssignmentException If no assignment with the given name exists.
     * @throws NoSuchGroupException If no group with the given name exists.
     * @throws StorageException If an exception occurred in the storage backend.
     * @throws UnauthorizedException If the given user is not authorized to access this information.
     */
    public List<Version> getVersions(String assignmentName, String groupName, String user)
            throws NoSuchAssignmentException, NoSuchGroupException, StorageException, UnauthorizedException {
        
        return storage.getVersions(assignmentName, groupName);
    }

    /**
     * Retrieves a previous submission.
     * 
     * @param assignmentName The name of the assignment.
     * @param groupName The name of the group
     * @param version The version to retrieve.
     * @param user The user that tries to access the submission.
     * 
     * @return The previous {@link Submission}.
     * 
     * @throws NoSuchAssignmentException If no assignment with the given name exists.
     * @throws NoSuchGroupException If no group with the given name exists.
     * @throws NoSuchVersionException If no such version exists.
     * @throws StorageException If an exception occurred in the storage backend.
     * @throws UnauthorizedException If the given user is not authorized to get the previous submission.
     */
    public Submission getSubmission(String assignmentName, String groupName, Version version, String user)
            throws NoSuchAssignmentException, NoSuchGroupException, NoSuchVersionException, 
            StorageException, UnauthorizedException {
        
        return storage.getSubmission(assignmentName, groupName, version);
    }
    
}
