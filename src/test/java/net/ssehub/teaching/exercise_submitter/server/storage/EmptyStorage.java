package net.ssehub.teaching.exercise_submitter.server.storage;

import java.util.Collections;
import java.util.List;

public class EmptyStorage implements ISubmissionStorage {

    @Override
    public void createOrUpdateAssignment(String course, String assignmentName, String... newGroupNames)
            throws StorageException {
    }

    @Override
    public void submitNewVersion(SubmissionTarget target, Submission submission)
            throws NoSuchTargetException, StorageException {
    }

    @Override
    public List<Version> getVersions(SubmissionTarget target) throws NoSuchTargetException, StorageException {
        return Collections.emptyList();
    }

    @Override
    public Submission getSubmission(SubmissionTarget target, Version version)
            throws NoSuchTargetException, StorageException {
        throw new NoSuchTargetException(target, version);
    }



}
