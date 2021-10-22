package net.ssehub.teaching.exercise_submitter.server.storage;

import java.util.List;

import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.ParticipantDto.RoleEnum;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Assignment;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Course;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Group;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Participant;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;

/**
 * Interface for the storage of submissions.
 * 
 * @author Adam
 */
public interface ISubmissionStorage {

    /**
     * Creates or updates the given assignment and groups within. This may be called multiple times for the same
     * assignment.
     * <p>
     * Note that groups of existing assignments are never deleted. Instead, all groups given here that do not exist
     * are created; groups that exist but are not given here are left as-is.
     * 
     * @param course The course identifier of the assignment.
     * @param assignmentName The name of the assignment in the given course.
     * @param newGroupNames The names of the groups to add.
     * 
     * @throws StorageException If an exception occurred in the storage backend.
     */
    public void createOrUpdateAssignment(String course, String assignmentName, String... newGroupNames)
            throws StorageException;
    
    /**
     * Adds a new submission for a given group and assignment.
     * 
     * @param target The assignment and group to add the submission for.
     * @param submission The submission to add.
     * 
     * @throws NoSuchTargetException If the given target does not exist.
     * @throws StorageException If an exception occurred in the storage backend.
     */
    public void submitNewVersion(SubmissionTarget target, Submission submission)
            throws NoSuchTargetException, StorageException;

    /**
     * Returns a list of all versions that have been submitted to the given assignment for the given group. The list
     * is sorted in reverse-chronological order, i.e. the first entry is the latest version. If no version was
     * submitted (yet), an empty list is returned.
     * 
     * @param target The assignment and group to add the submission for.
     * 
     * @return The list of all versions in reverse-chronological order.
     * 
     * @throws NoSuchTargetException If the given target does  not exist.
     * @throws StorageException If an exception occurred in the storage backend.
     */
    public List<Version> getVersions(SubmissionTarget target)
            throws NoSuchTargetException, StorageException;
    
    /**
     * Retrieves the submission of the given assignment and group.
     * 
     * @param target The assignment and group to get the submission for.
     * @param version The version of the submission to get.
     * 
     * @return The submission.
     * 
     * @throws NoSuchTargetException If the given target or version does not exist.
     * @throws StorageException If an exception occurred in the storage backend.
     */
    public Submission getSubmission(SubmissionTarget target, Version version)
            throws NoSuchTargetException, StorageException;
    
    /**
     * Calls {@link #createOrUpdateAssignment(String, String, String...)} as necessary to create all assignments and
     * groups.
     * 
     * @param view The student management system view to create everything for.
     * 
     * @throws StorageException If an exception occurred in the storage backend.
     */
    public default void createOrUpdateAssignmentsFromView(StuMgmtView view) throws StorageException {
        
        for (Course course : view.getCourses()) {
            for (Assignment assignment : course.getAssignments()) {
                if (assignment.getCollaboration() == CollaborationEnum.SINGLE
                        || assignment.getCollaboration() == CollaborationEnum.GROUP_OR_SINGLE) {
                    
                    createOrUpdateAssignment(course.getId(), assignment.getName(), course.getParticipants().stream()
                            .filter(p -> p.getRole() == RoleEnum.STUDENT)
                            .map(Participant::getName)
                            .toArray(s -> new String[s]));
                }
                
                if (assignment.getCollaboration() == CollaborationEnum.GROUP
                        || assignment.getCollaboration() == CollaborationEnum.GROUP_OR_SINGLE) {
                    
                    createOrUpdateAssignment(course.getId(), assignment.getName(), assignment.getGroups().stream()
                            .map(Group::getName)
                            .toArray(s -> new String[s]));
                }
            }
        }
    }
    
}
