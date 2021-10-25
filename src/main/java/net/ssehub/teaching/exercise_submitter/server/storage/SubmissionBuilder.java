package net.ssehub.teaching.exercise_submitter.server.storage;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * A builder for creating {@link Submission}s.
 * 
 * @author Adam
 */
public class SubmissionBuilder {

    private boolean built;
    
    private String author;
    
    private Map<Path, byte[]> files;
    
    /**
     * Creates a new builder with no files (yet).
     * 
     * @param author The name of the author that created this submission.
     */
    public SubmissionBuilder(String author) {
        this.author = author;
        this.files = new HashMap<>();
        this.built = false;
    }
    
    /**
     * Adds a file.
     * 
     * @param filepath The relative path of the file in the submission directory.
     * @param content The content of the file.
     * 
     * @throws IllegalArgumentException If the given filepath is not relative.
     * @throws IllegalStateException If {@link #build()} has already been called on this builder.
     */
    public void addFile(Path filepath, byte[] content) throws IllegalArgumentException, IllegalStateException {
        checkNotBuilt();
        
        if (filepath.isAbsolute()) {
            throw new IllegalArgumentException(filepath + " is absolute");
        }
        
        for (Path element : filepath) {
            if (element.toString().equals("..")) {
                throw new IllegalArgumentException(".. is not allowed in submission paths");
            }
        }
        
        this.files.put(filepath, content);
    }
    
    /**
     * Same as {@link #addFile(Path, byte[])}, but file content is the given string and will be UTF-8 encoded.
     * <p>
     * This is a convenience method for test cases.
     *  
     * @param filepath The relative path of the file in the submission directory.
     * @param content The string content of the file.
     * 
     * @throws IllegalArgumentException If the given filepath is not relative.
     * @throws IllegalStateException If {@link #build()} has already been called on this builder.
     */
    public void addUtf8File(Path filepath, String content)  throws IllegalArgumentException, IllegalStateException {
        addFile(filepath, content.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Creates the {@link Submission}.
     * 
     * @return A {@link Submission} with all the previously added files (see {@link #addFile(Path, String)}).
     * 
     * @throws IllegalStateException If {@link #build()} has already been called on this builder.
     */
    public Submission build() throws IllegalStateException {
        checkNotBuilt();
        this.built = true;
        return new Submission(author, files);
    }
    
    /**
     * Ensures that {@link #built} is <code>false</code>.
     * 
     * @throws IllegalStateException If {@link #built} is not <code>false</code>.
     */
    private void checkNotBuilt() throws IllegalStateException {
        if (this.built) {
            throw new IllegalStateException("build() was already called");
        }
    }
    
}
