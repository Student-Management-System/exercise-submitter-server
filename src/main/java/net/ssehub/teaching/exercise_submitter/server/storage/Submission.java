package net.ssehub.teaching.exercise_submitter.server.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Represents a submission, i.e. a collection of files with relative path and content. Use {@link SubmissionBuilder}
 * to create instances.
 * 
 * @author Adam
 */
public class Submission {

    private String author;
    
    private Map<Path, String> files;
    
    /**
     * Creates a submission. Called by {@link SubmissionBuilder}.
     * 
     * @param author The author of the submission.
     * @param files The files.
     */
    Submission(String author, Map<Path, String> files) {
        this.author = author;
        this.files = files;
    }
    
    /**
     * Returns the name of the author that creates this submission.
     * 
     * @return The author of this submission.
     */
    public String getAuthor() {
        return author;
    }
    
    /**
     * Returns a set with all filepaths in this submission. The filepaths are relative paths inside the submission
     * directory. This does not include entries for directories; the directories are implicitly contained in the
     * relative paths of the files.
     * 
     * @return A set of all filenames. 
     */
    public Set<Path> getFilepaths() {
        return files.keySet();
    }
    
    /**
     * Returns the number of files that are in this submission.
     * 
     * @return The number of files.
     */
    public int getNumFiles() {
        return files.size();
    }
    
    /**
     * Checks if a file with the given file (relative path inside the submission directory) exists.
     * 
     * @param filepath The file to check.
     * 
     * @return Whether such a file is contained in this submission.
     */
    public boolean containsFile(Path filepath) {
        return files.containsKey(filepath);
    }
    
    /**
     * Returns the content of the given file.
     * 
     * @param filepath The relative path of the file in the submission directory.
     * 
     * @return The content of the file.
     * 
     * @throws NoSuchElementException If the file does not exist in this submission.
     */
    public String getFileContent(Path filepath) throws NoSuchElementException {
        if (!containsFile(filepath)) {
            throw new NoSuchElementException("File " + filepath + " does not exist in this submission");
        }
        return files.get(filepath);
    }
    
    /**
     * Writes all files of this submission to a given directory. Existing files in the given directory are overridden if
     * a file with the same name exist in this submission. Files are written in UTF-8 encoding. Sub-directories are
     * created as necessary.
     * 
     * @param directory The directory to write this submission to. Must be an existing directory.
     * 
     * @throws IOException If the given directory is not an existing directory or writing the files fails. 
     */
    public void writeToDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IOException(directory + " is not a directory");
        }
        
        for (Map.Entry<Path, String> file : this.files.entrySet()) {
            writeFile(file.getKey(), file.getValue(), directory);
        }
    }
    
    /**
     * Writes a single file to a given directory. Writes in UTF-8 encoding. Sub-directories are created as necessary.
     * 
     * @param filepath The relative path of the file in the directory.
     * @param content The content of the file.
     * @param directory The directory to write the file to.
     * 
     * @throws IOException If writing the file or creating the parent directories fails.
     */
    private void writeFile(Path filepath, String content, Path directory) throws IOException {
        Path absoluteDestination = directory.resolve(filepath);
        Files.createDirectories(absoluteDestination.getParent());
        Files.writeString(absoluteDestination, content, StandardCharsets.UTF_8);
    }
    
}
