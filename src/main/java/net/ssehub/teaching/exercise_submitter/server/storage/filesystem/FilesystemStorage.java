package net.ssehub.teaching.exercise_submitter.server.storage.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.ssehub.teaching.exercise_submitter.server.storage.AssignmentAlreadyExistsException;
import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchAssignmentException;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchGroupException;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchVersionException;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.Submission;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionBuilder;
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
     * @param assignmentName The name of the assignment.
     * 
     * @return The path to the assignment in the {@link #baseDirectory}.
     */
    private Path getAssignmentPath(String assignmentName) {
        return baseDirectory.resolve(assignmentName);
    }
    
    /**
     * Creates the path to the given assignment. Also checks that the assignment directory exists.
     * 
     * @param assignmentName The name of the assignment.
     * 
     * @return The path to the assigment in the {@link #baseDirectory}.
     * 
     * @throws NoSuchAssignmentException If the assignment does not exist.
     */
    private Path getExistingAssignmentPath(String assignmentName) throws NoSuchAssignmentException {
        Path assignmentDir = getAssignmentPath(assignmentName);
        if (!Files.isDirectory(assignmentDir)) {
            throw new NoSuchAssignmentException(assignmentName);
        }
        return assignmentDir;
    }
    
    /**
     * Creates the path to the given group directory in the given assignment. Also checks that the assignment and group
     * directories exist.
     * 
     * @param assignmentName The name of the assignment.
     * @param groupName The name of the group.
     * 
     * @return The path to the group folder in the given assignment.
     * 
     * @throws NoSuchAssignmentException If the assignment does not exist.
     * @throws NoSuchGroupException If the group does not exist.
     */
    private Path getExistingGroupPath(String assignmentName, String groupName)
            throws NoSuchAssignmentException, NoSuchGroupException {

        Path groupDir = getExistingAssignmentPath(assignmentName).resolve(groupName);
        if (!Files.isDirectory(groupDir)) {
            throw new NoSuchGroupException(assignmentName, groupName);
        }
        return groupDir;
    }
    
    /**
     * Returns the set of all filenames in the given directory. Includes file and directory names. Does not recurse.
     * 
     * @param directory The directory to get the filenames for.
     * 
     * @return A set of all filenames immediately in that directory.
     * 
     * @throws StorageException If getting the directory content fails.
     */
    private Set<String> getFilenames(Path directory) throws StorageException {
        try {
            return Files.list(directory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }
    
    @Override
    public void createAssignment(String assignmentName) throws AssignmentAlreadyExistsException, StorageException {
        try {
            Path assignmentDir = getAssignmentPath(assignmentName);
            
            if (Files.exists(assignmentDir)) {
                throw new AssignmentAlreadyExistsException(assignmentName);
            }
            
            Files.createDirectory(assignmentDir);
            
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void addGroupsToAssignment(String assignmentName, String... groupNames)
            throws NoSuchAssignmentException, StorageException {
        
        Path assignmentDir = getExistingAssignmentPath(assignmentName);
        
        for (String group : groupNames) {
            Path groupDir = assignmentDir.resolve(group);
            try {
                if (!Files.exists(groupDir)) {
                    Files.createDirectory(groupDir);
                }
            } catch (IOException e) {
                throw new StorageException(e);
            }
        }
    }

    @Override
    public void submitNewVersion(String assignmentName, String groupName, String author, Submission submission)
            throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
        
        Path groupDir = getExistingGroupPath(assignmentName, groupName);
        
        Version newVersion = new Version(author, LocalDateTime.now());
        
        Path versionDir = groupDir.resolve(versionToFilename(newVersion));
        if (Files.isDirectory(versionDir)) {
            throw new StorageException("Version already exists");
        }
        
        try {
            Files.createDirectory(versionDir);
            submission.writeToDirectory(versionDir);
            
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public Set<String> getAssignments() throws StorageException {
        return getFilenames(baseDirectory);
    }

    @Override
    public Set<String> getGroups(String assignmentName) throws NoSuchAssignmentException, StorageException {
        Path assignmentDir = getExistingAssignmentPath(assignmentName);
        return getFilenames(assignmentDir);
    }

    @Override
    public List<Version> getVersions(String assignmentName, String groupName)
            throws NoSuchAssignmentException, NoSuchGroupException, StorageException {
        
        Path groupDir = getExistingGroupPath(assignmentName, groupName);
        
        try {
            return Files.list(groupDir)
                .map(p -> p.getFileName().toString())
                .map(FilesystemStorage::filenameToVersion)
                .sorted(Comparator.comparing(Version::getTimestamp).reversed())
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
        
        return new Version(author, unixtimestamp);
    }
    
    /**
     * Converts the given {@link Version} to a filename. Inverse operation to {@link #filenameToVersion(String)}.
     * 
     * @param version The version to create a filename for.
     * 
     * @return A filename representing the given version.
     */
    static String versionToFilename(Version version) {
        return version.getUnixTimestamp() + "_" + version.getAuthor();
    }
    
    @Override
    public Submission getSubmission(String assignmentName, String groupName, Version version)
            throws NoSuchAssignmentException, NoSuchGroupException, NoSuchVersionException, StorageException {
        
        Path groupDir = getExistingGroupPath(assignmentName, groupName);
        
        Path versionDir = groupDir.resolve(versionToFilename(version));
        if (!Files.isDirectory(versionDir)) {
            throw new NoSuchVersionException(assignmentName, groupName, version);
        }
        
        SubmissionBuilder builder = new SubmissionBuilder();
        
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
