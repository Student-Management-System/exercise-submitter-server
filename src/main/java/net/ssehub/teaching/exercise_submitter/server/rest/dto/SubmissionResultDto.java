package net.ssehub.teaching.exercise_submitter.server.rest.dto;

import net.ssehub.teaching.exercise_submitter.server.checks.Check;

/**
 * Represents the result of a submission.
 * 
 * @author Adam
 */
public class SubmissionResultDto {

    private boolean accepted;
    
    /**
     * Sets whether the submission was accepted or rejected based on {@link Check}s.
     * 
     * @param accepted Whether the submission was accepted.
     */
    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }
    
    /**
     * Gets whether the submission was accepted or rejected based on {@link Check}s.
     * 
     * @return Whether the submission was accepted.
     */
    public boolean getAccepted() {
        return accepted;
    }
    
}
