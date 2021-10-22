package net.ssehub.teaching.exercise_submitter.server.storage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Pointer to a specific submitted version. Contains the timestamp and author name.
 * 
 * @author Adam
 */
public class Version {

    private String author;
    
    private LocalDateTime timestamp;
    
    /**
     * Creates a pointer to a version.
     * 
     * @param author The name of the author of this version.
     * @param timestamp The timestamp when this version was created.
     */
    public Version(String author, LocalDateTime timestamp) {
        this.author = author;
        this.timestamp = timestamp;
    }
    
    /**
     * Creates a pointer to a version.
     * 
     * @param author The name of the author of this version.
     * @param unixTimestamp Seconds since unix epoch.
     */
    public Version(String author, long unixTimestamp) {
        this.author = author;
        this.timestamp = LocalDateTime.ofInstant(Instant.ofEpochSecond(unixTimestamp), ZoneId.systemDefault());
    }
    
    /**
     * Returns the name of the author of this version.
     * 
     * @return The author.
     */
    public String getAuthor() {
        return author;
    }
    
    /**
     * Returns the timestamp when the version was created.
     * 
     * @return The timestamp.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Returns the timestamp as seconds since unix epoch.
     * 
     * @return The unix timestamp.
     */
    public long getUnixTimestamp() {
        return timestamp.atZone(ZoneId.systemDefault()).toEpochSecond();
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(author, timestamp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Version)) {
            return false;
        }
        Version other = (Version) obj;
        return Objects.equals(author, other.author) && Objects.equals(timestamp, other.timestamp);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Version [author=");
        builder.append(author);
        builder.append(", timestamp=");
        builder.append(timestamp);
        builder.append("]");
        return builder.toString();
    }
    
}
