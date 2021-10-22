package net.ssehub.teaching.exercise_submitter.server.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a version.
 * 
 * @author Adam
 */
public class VersionDto {

    private String author;
    
    @Schema(description = "the timestamp when the version was created as seconds since unix epoch")
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
