package net.ssehub.teaching.exercise_submitter.server.storage;

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
    
    private Map<Path, String> files;
    
    /**
     * Creates a new builder with no files (yet).
     */
    public SubmissionBuilder() {
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
    public void addFile(Path filepath, String content) throws IllegalArgumentException, IllegalStateException {
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
     * Creates the {@link Submission}.
     * 
     * @return A {@link Submission} with all the previously added files (see {@link #addFile(Path, String)}).
     * 
     * @throws IllegalStateException If {@link #build()} has already been called on this builder.
     */
    public Submission build() throws IllegalStateException {
        checkNotBuilt();
        this.built = true;
        return new Submission(files);
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
