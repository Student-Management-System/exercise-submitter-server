package net.ssehub.teaching.exercise_submitter.server.storage.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchTargetException;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.Submission;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionBuilder;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.storage.Version;

/**
 * An implementation that stores the submissions on the regular filesystem. For each new submitted version, a
 * sub-folder named after the unix-timestamp and the author name is created.
 *  
 * @author Adam
 */
public class FilesystemStorage implements ISubmissionStorage {
    
    private Path baseDirectory;
    
    /**
     * Creates a new {@link FilesystemStorage} with the given base directory. The directory must exist. If it is not
     * empty, the content must conform to the layout of this filesystem storage.
     * 
     * @param baseDirectory The base directory.
     * 
     * @throws IOException If the given base directory is not a directory.
     */
    public FilesystemStorage(Path baseDirectory) throws IOException {
        if (!Files.isDirectory(baseDirectory)) {
            throw new IOException(baseDirectory +  " is not a directory");
        }
        
        this.baseDirectory = baseDirectory;
    }
    
    /**
     * Creates the path to the given assignment. Does no checks whether this exists.
     * 
     * @param course The course identifier that the assignment belongs to.
     * @param assignmentName The name of the assignment.
     * 
     * @return The path to the assignment in the {@link #baseDirectory}.
     */
    private Path getAssignmentPath(String course, String assignmentName) {
        return baseDirectory.resolve(Path.of(course, assignmentName));
    }
    
    /**
     * Creates the path to a group directory inside an assignment. Also checks that the directory exists.
     * 
     * @param target The target that specifies course, assignment, and group.
     * 
     * @return The path to the group directory inside the given assignment.
     * 
     * @throws NoSuchTargetException If the target directory does not exist.
     */
    private Path getExistingGroupPath(SubmissionTarget target) throws NoSuchTargetException {
        Path path = baseDirectory.resolve(
                Path.of(target.getCourse(), target.getAssignmentName(), target.getGroupName()));
        if (!Files.isDirectory(path)) {
            throw new NoSuchTargetException(target);
        }
        return path;
    }
    
    @Override
    public void createOrUpdateAssignment(String course, String assignmentName, String... newGroupNames)
            throws StorageException {
        
        try {
            Path assignmentPath = getAssignmentPath(course, assignmentName);
            if (!Files.isDirectory(assignmentPath)) {
                Files.createDirectories(assignmentPath);
            }
            
            for (String groupName : newGroupNames) {
                Path groupDir = assignmentPath.resolve(groupName);
                if (!Files.isDirectory(groupDir)) {
                    Files.createDirectory(groupDir);
                }
            }
            
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }
    

    @Override
    public void submitNewVersion(SubmissionTarget target, Submission submission)
            throws NoSuchTargetException, StorageException {
        
        Path groupDir = getExistingGroupPath(target);
        
        Version newVersion = new Version(submission.getAuthor(), Instant.now());
        long newTimestamp = newVersion.getCreationTime().getEpochSecond();
        
        try {
            boolean versionAlreadyExists = Files.list(groupDir)
                .map(Path::getFileName)
                .map(Path::toString)
                .anyMatch(filename -> filename.startsWith(Long.toString(newTimestamp) + '_'));
            if (versionAlreadyExists) {
                throw new StorageException("Version already exists");
            }
        
            Path versionDir = groupDir.resolve(versionToFilename(newVersion));
            Files.createDirectory(versionDir);
            submission.writeToDirectory(versionDir);
            
        } catch (IOException e) {
            throw new StorageException(e);
        }
        
    }
    
    @Override
    public List<Version> getVersions(SubmissionTarget target) throws NoSuchTargetException, StorageException {
        Path groupDir = getExistingGroupPath(target);
        
        try {
            return Files.list(groupDir)
                    .map(p -> p.getFileName().toString())
                    .map(FilesystemStorage::filenameToVersion)
                    .sorted(Comparator.comparing(Version::getCreationTime).reversed())
                    .collect(Collectors.toList());
            
        } catch (IllegalArgumentException | IOException e) {
            throw new StorageException(e);
        }
    }
    
    /**
     * Converts a filename to a {@link Version}. The filename must have the format:
     * <code><i>timestamp</i>_<i>author</i></code>, where <code><i>timestamp</i></code> is the Unix epoch timestamp
     * (in seconds) at UTC, and <code><i>author</i></code> is the name of the author.
     * <p>
     * Package visibility for test cases.
     * 
     * @param filename The filename to parse the version from.
     * 
     * @return The {@link Version} represented by the given filename.
     * 
     * @throws IllegalArgumentException If the filename is malformed.
     */
    static Version filenameToVersion(String filename) throws IllegalArgumentException {
        int underscore = filename.indexOf('_');
        if (underscore == -1) {
            throw new IllegalArgumentException("Missing _ in version");
        }
        if (underscore == filename.length() - 1) {
            throw new IllegalArgumentException("No author");
        }
        
        long unixtimestamp = Long.parseLong(filename.substring(0, underscore));
        String author = filename.substring(underscore + 1);
        
        return new Version(author, Instant.ofEpochSecond(unixtimestamp));
    }
    
    /**
     * Converts the given {@link Version} to a filename. Inverse operation to {@link #filenameToVersion(String)}.
     * 
     * @param version The version to create a filename for.
     * 
     * @return A filename representing the given version.
     */
    static String versionToFilename(Version version) {
        return version.getCreationTime().getEpochSecond() + "_" + version.getAuthor();
    }
    
    @Override
    public Submission getSubmission(SubmissionTarget target, Version version)
            throws NoSuchTargetException, StorageException {
        
        Path groupDir = getExistingGroupPath(target);
        
        Path versionDir = groupDir.resolve(versionToFilename(version));
        if (!Files.isDirectory(versionDir)) {
            throw new NoSuchTargetException(target, version);
        }
        
        SubmissionBuilder builder = new SubmissionBuilder(version.getAuthor());
        
        try {
            Files.walk(versionDir)
                .filter(Files::isRegularFile)
                .forEach(file -> {
                    try {
                        builder.addFile(versionDir.relativize(file), Files.readString(file, StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            
            return builder.build();
            
        } catch (UncheckedIOException e) {
            throw new StorageException(e.getCause());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

}
