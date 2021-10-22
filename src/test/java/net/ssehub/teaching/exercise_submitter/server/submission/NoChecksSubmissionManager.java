package net.ssehub.teaching.exercise_submitter.server.submission;

import net.ssehub.teaching.exercise_submitter.server.rest.dto.SubmissionResultDto;
import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchTargetException;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.Submission;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;

public class NoChecksSubmissionManager extends SubmissionManager {

    private ISubmissionStorage storage;
    
    public NoChecksSubmissionManager(ISubmissionStorage storage) {
        super(storage);
        this.storage = storage;
    }

    @Override
    public SubmissionResultDto submit(SubmissionTarget target, Submission submission)
            throws NoSuchTargetException, StorageException {
        storage.submitNewVersion(target, submission);
        
        SubmissionResultDto result = new SubmissionResultDto();
        result.setAccepted(true);
        return result;
    }
    
}
