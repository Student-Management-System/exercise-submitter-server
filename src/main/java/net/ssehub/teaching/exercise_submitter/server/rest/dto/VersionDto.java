package net.ssehub.teaching.exercise_submitter.server.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a version.
 * 
 * @author Adam
 */
@Schema(description = "A version of a submission")
public class VersionDto {

    @Schema(
        description = "The username of the author that created the submission",
        required = true,
        example = "student1"
    )
    private String author;
    
    @Schema(
        description = "The timestamp when the version was created, as seconds since unix epoch",
        required = true,
        example = "1635177322"
    )
    private long timestamp;
    
    /**
     * Sets the author of the version.
     * 
     * @param author The author of the version.
     */
    public void setAuthor(String author) {
        this.author = author;
    }
    
    
    /**
     * Gets the author of the version.
     * 
     * @return The author of the version.
     */
    public String getAuthor() {
        return author;
    }
    
    /**
     * Sets the Unix timestamp of the version.
     * 
     * @param timestamp The Unix timestamp.
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * Gets the Unix timestamp of the version.
     * 
     * @return The Unix timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }
    
}
