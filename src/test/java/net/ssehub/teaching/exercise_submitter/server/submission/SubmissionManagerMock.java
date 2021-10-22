package net.ssehub.teaching.exercise_submitter.server.submission;

import java.util.List;

import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchTargetException;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.Submission;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.storage.Version;

public class SubmissionManagerMock extends SubmissionManager {

    private ISubmissionStorage storage;
    
    public SubmissionManagerMock(ISubmissionStorage storage) {
        super(storage);
        this.storage = storage;
    }

    @Override
    public void submit(SubmissionTarget target, Submission submission)
            throws NoSuchTargetException, StorageException, UnauthorizedException {
        storage.submitNewVersion(target, submission);
    }
    
    @Override
    public List<Version> getVersions(SubmissionTarget target, String user)
            throws NoSuchTargetException, StorageException, UnauthorizedException {
        return storage.getVersions(target);
    }
    
    @Override
    public Submission getSubmission(SubmissionTarget target, Version version, String user)
            throws NoSuchTargetException, StorageException, UnauthorizedException {
        return storage.getSubmission(target, version);
    }
    
    @Override
    public String authenticate(String token) throws UnauthorizedException {
        return "user";
    }
    
}
