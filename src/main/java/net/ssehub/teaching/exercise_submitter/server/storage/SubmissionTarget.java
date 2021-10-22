package net.ssehub.teaching.exercise_submitter.server.storage;

import java.util.Objects;

/**
 * Represents a target for a submission, including course, assignment, and group.
 * 
 * @author Adam
 */
public class SubmissionTarget {

    private String course;
    
    private String assignmentName;
    
    private String groupName;

    /**
     * Creates a new target.
     * 
     * @param course The identifier for the course that the assignment is in.
     * @param assignmentName The name of the assignment.
     * @param groupName The name of the group.
     */
    public SubmissionTarget(String course, String assignmentName, String groupName) {
        this.course = course;
        this.assignmentName = assignmentName;
        this.groupName = groupName;
    }

    /**
     * Returns the identifier of the course that the assignment is in.
     * 
     * @return The course identifier.
     */
    public String getCourse() {
        return course;
    }

    /**
     * Returns the name of the assignment that is targeted.
     * 
     * @return The assignment name.
     */
    public String getAssignmentName() {
        return assignmentName;
    }

    /**
     * Returns the name of the group in the assignment that is targeted.
     * 
     * @return The group name.
     */
    public String getGroupName() {
        return groupName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentName, course, groupName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SubmissionTarget)) {
            return false;
        }
        SubmissionTarget other = (SubmissionTarget) obj;
        return Objects.equals(assignmentName, other.assignmentName) && Objects.equals(course, other.course)
                && Objects.equals(groupName, other.groupName);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SubmissionTarget [course=");
        builder.append(course);
        builder.append(", assignmentName=");
        builder.append(assignmentName);
        builder.append(", groupName=");
        builder.append(groupName);
        builder.append("]");
        return builder.toString();
    }
    
}
