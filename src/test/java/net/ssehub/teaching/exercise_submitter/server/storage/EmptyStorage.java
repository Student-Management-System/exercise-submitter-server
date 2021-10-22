package net.ssehub.teaching.exercise_submitter.server.storage;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class EmptyStorage implements ISubmissionStorage {

    @Override
    public void createAssignment(String assignmentName) throws AssignmentAlreadyExistsException, StorageException {
    }

    @Override
    public void addGroupsToAssignment(String assignmentName, String... groupNames)
            throws NoSuchAssignmentException, StorageException {
    }

    @Override
    public void submitNewVersion(String assignmentName, String groupName, String author, Submission submission)
            throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
    }

    @Override
    public Set<String> getAssignments() throws StorageException {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getGroups(String assignmentName) throws NoSuchAssignmentException, StorageException {
        return Collections.emptySet();
    }

    @Override
    public List<Version> getVersions(String assignmentName, String groupName)
            throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
        return Collections.emptyList();
    }

    @Override
    public Submission getSubmission(String assignmentName, String groupName, Version version)
            throws NoSuchAssignmentException, NoSuchGroupException, NoSuchVersionException, StorageException {
        throw new NoSuchVersionException(assignmentName, groupName, version);
    }

}
