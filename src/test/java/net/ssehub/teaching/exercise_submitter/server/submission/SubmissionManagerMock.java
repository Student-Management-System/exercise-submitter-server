package net.ssehub.teaching.exercise_submitter.server.submission;

import java.util.List;

import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchAssignmentException;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchGroupException;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchVersionException;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.Submission;
import net.ssehub.teaching.exercise_submitter.server.storage.Version;

public class SubmissionManagerMock extends SubmissionManager {

    private ISubmissionStorage storage;
    
    public SubmissionManagerMock(ISubmissionStorage storage) {
        super(storage);
        this.storage = storage;
    }
    
    @Override
    public Submission getSubmission(String assignmentName, String groupName, Version version, String user)
            throws NoSuchAssignmentException, NoSuchGroupException, NoSuchVersionException, StorageException,
            UnauthorizedException {
        return storage.getSubmission(assignmentName, groupName, version);
    }
    
    @Override
    public List<Version> getVersions(String assignmentName, String groupName, String author)
            throws NoSuchAssignmentException, NoSuchGroupException, StorageException, UnauthorizedException {
        return storage.getVersions(assignmentName, groupName);
    }
    
    @Override
    public void submit(String assignmentName, String groupName, String author, Submission submission)
            throws NoSuchAssignmentException, NoSuchGroupException, StorageException, UnauthorizedException {
        storage.submitNewVersion(assignmentName, groupName, author, submission);
    }

}
