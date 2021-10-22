package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A course in the student management system.
 * 
 * @author Adam
 */
public class Course {

    private String id;
    
    private Map<String, Participant> participantsByName;
    
    private Map<String, Assignment> assignmentsByName;
    
    /**
     * Creates a course.
     * 
     * @param id The identifier of the course.
     */
    Course(String id) {
        this.id = id;
        this.participantsByName = new HashMap<>();
        this.assignmentsByName = new HashMap<>();
    }
    
    /**
     * Returns the identifier of this course.
     * 
     * @return The course identifier.
     */
    public String getId() {
        return id;
    }
    
    /**
     * Retrieves a participant by its name.
     * 
     * @param name The name of the participant.
     * 
     * @return The participant, or {@link Optional#empty()} if no such assignment exists.
     */
    public Optional<Participant> getParticipant(String name) {
        return Optional.ofNullable(participantsByName.get(name));
    }
    
    /**
     * Returns all participants in this course.
     * 
     * @return All participants as an unmodifiable collection.
     */
    public Collection<Participant> getParticipants() {
        return Collections.unmodifiableCollection(this.participantsByName.values());
    }
    
    /**
     * Retrieves an assignment by its name.
     * 
     * @param name The name of the assignment.
     * 
     * @return The assignment, or {@link Optional#empty()} if no such assignment exists.
     */
    public Optional<Assignment> getAssignment(String name) {
        return Optional.ofNullable(assignmentsByName.get(name));
    }
    
    /**
     * Returns all assignments in this course.
     * 
     * @return All assignments as an unmodifiable collection.
     */
    public Collection<Assignment> getAssignments() {
        return Collections.unmodifiableCollection(this.assignmentsByName.values());
    }

    /**
     * Adds a participant to this course.
     * 
     * @param participant The participant to add.
     */
    void addParticipant(Participant participant) {
        this.participantsByName.put(participant.getName(), participant);
    }
    
    /**
     * Adds an assignment to this course.
     * 
     * @param assignment The assignment to add.
     */
    void addAssignment(Assignment assignment) {
        this.assignmentsByName.put(assignment.getName(), assignment);
    }
    
}
