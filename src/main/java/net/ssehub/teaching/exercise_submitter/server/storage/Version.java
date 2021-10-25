package net.ssehub.teaching.exercise_submitter.server.storage;

import java.time.Instant;
import java.util.Objects;

/**
 * Pointer to a specific submitted version. Contains the timestamp and author name.
 * 
 * @author Adam
 */
public class Version {

    private String author;
    
    private Instant creationTime;
    
    /**
     * Creates a pointer to a version.
     * 
     * @param author The name of the author of this version.
     * @param creationTime The timestamp when this version was created.
     */
    public Version(String author, Instant creationTime) {
        this.author = author;
        this.creationTime = creationTime;
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
    public Instant getCreationTime() {
        return creationTime;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(author, creationTime);
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
        return Objects.equals(author, other.author) && Objects.equals(creationTime, other.creationTime);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Version [author=");
        builder.append(author);
        builder.append(", creationTime=");
        builder.append(creationTime.getEpochSecond());
        builder.append("]");
        return builder.toString();
    }
    
}
