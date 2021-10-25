package net.ssehub.teaching.exercise_submitter.server.rest.dto;

import java.util.List;

import net.ssehub.teaching.exercise_submitter.server.submission.checks.Check;

/**
 * Represents the result of a submission.
 * 
 * @author Adam
 */
public class SubmissionResultDto {

    private boolean accepted;
    
    private List<CheckMessageDto> messages;
    
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
    
    
    /**
     * Sets the messages from he automated checks.
     * 
     * @param messages The messages.
     */
    public void setMessages(List<CheckMessageDto> messages) {
        this.messages = messages;
    }
    
    /**
     * Gets the messages from the automated checks.
     * 
     * @return The messages.
     */
    public List<CheckMessageDto> getMessages() {
        return messages;
    }
    
}
