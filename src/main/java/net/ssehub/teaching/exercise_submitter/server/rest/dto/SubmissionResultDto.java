package net.ssehub.teaching.exercise_submitter.server.rest.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.Check;

/**
 * Represents the result of a submission.
 * 
 * @author Adam
 */
@Schema(description = "The result of a submission")
public class SubmissionResultDto {

    @Schema(description = "Whether the submission was accepted or rejected by automatic checks", required = true)
    private boolean accepted;
    
    @Schema(description = "Messages created by automatic checks on the submission", required = true)
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
