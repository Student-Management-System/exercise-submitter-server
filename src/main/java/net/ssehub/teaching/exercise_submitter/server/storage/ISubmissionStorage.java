package net.ssehub.teaching.exercise_submitter.server.storage;

import java.util.List;
import java.util.Set;

/**
 * Interface for the storage of submissions.
 * 
 * @author Adam
 */
public interface ISubmissionStorage {

    /**
     * Creates a new assignment.
     * 
     * @param assignmentName The name of the new assignment.
     * 
     * @throws AssignmentAlreadyExistsException If an assignment with that name already exists.
     * @throws StorageException If an exception occurred in the storage backend.
     */
    public void createAssignment(String assignmentName)
            throws AssignmentAlreadyExistsException, StorageException;
    
    /**
     * Adds groups to the given assignment. This may be called multiple times. Group names that already exists are
     * silently ignored.
     * 
     * @param assignmentName The name of the assignment to add the groups to.
     * @param groupNames The names of the groups to add.
     * 
     * @throws NoSuchAssignmentException If an assignment with the given name does not exist.
     * @throws StorageException If an exception occurred in the storage backend.
     */
    public void addGroupsToAssignment(String assignmentName, String... groupNames)
            throws NoSuchAssignmentException, StorageException;
    
    /**
     * Adds a new submission for a given group and assignment.
     * 
     * @param assignmentName The name of the assignment to add the submission to.
     * @param groupName The name of the group to add the submission for.
     * @param author The name of the author of the new submission.
     * @param submission The submission to add.
     * 
     * @throws NoSuchAssignmentException If no assignment with the given name exists.
     * @throws NoSuchGroupException If no group with the given name exists.
     * @throws StorageException If an exception occurred in the storage backend.
     */
    public void submitNewVersion(String assignmentName, String groupName, String author, Submission submission)
            throws NoSuchAssignmentException, NoSuchGroupException, StorageException;

    /**
     * Returns a set of all assignment names.
     * 
     * @return All assignment names.
     * 
     * @throws StorageException If an exception occurred in the storage backend.
     */
    public Set<String> getAssignments()
            throws StorageException;
    
    /**
     * Returns a set of all group names for a given assignment.
     * 
     * @param assignmentName The name of the assignment to get the group names for.
     * 
     * @return All group names.
     * 
     * @throws NoSuchAssignmentException If no assignment with the given name exists.
     * @throws StorageException If an exception occurred in the storage backend.
     */
    public Set<String> getGroups(String assignmentName)
            throws NoSuchAssignmentException, StorageException;
    
    /**
     * Returns a list of all versions that have been submitted to the given assignment for the given group. The list
     * is sorted in reverse-chronological order, i.e. the first entry is the latest version. If no version was
     * submitted (yet), an empty list is returned.
     * 
     * @param assignmentName The name of the assignment.
     * @param groupName The name of the group.
     * 
     * @return The list of all versions in reverse-chronological order.
     * 
     * @throws NoSuchAssignmentException If no assignment with the given name exists.
     * @throws NoSuchGroupException If no group with the given name exists.
     * @throws StorageException If an exception occurred in the storage backend.
     */
    public List<Version> getVersions(String assignmentName, String groupName)
            throws NoSuchAssignmentException, NoSuchGroupException, StorageException;
    
    /**
     * Retrieves the submission of the given assignment and group.
     * 
     * @param assignmentName The name of the assignment.
     * @param groupName The name of the group.
     * @param version The version of the submission to get.
     * 
     * @return The submission.
     * 
     * @throws NoSuchAssignmentException If no assignment with the given name exists.
     * @throws NoSuchGroupException If no group with the given name exists.
     * @throws NoSuchVersionException If no such version exists.
     * @throws StorageException If an exception occurred in the storage backend.
     */
    public Submission getSubmission(String assignmentName, String groupName, Version version)
            throws NoSuchAssignmentException, NoSuchGroupException, NoSuchVersionException, StorageException;
    
    
}
