/*
 * Copyright 2020 Software Systems Engineering, University of Hildesheim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ssehub.teaching.exercise_submitter.server.submission.checks;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Static utility methods for files and directories.
 * 
 * @author Adam
 */
public class FileUtils {

    private static boolean fileOperationsShouldFail;
    
    /**
     * Don't allow any instances.
     */
    private FileUtils() {}
    
    /**
     * Recurses through the given directory and finds all files (not directories) which name ends with the given suffix.
     * 
     * @param directory The directory to search files in.
     * @param suffix The suffix to search for.
     * 
     * @return The set of all matching files.
     * 
     * @throws IOException If searching through the directory fails.
     */
    public static Set<Path> findFilesBySuffix(Path directory, String suffix) throws IOException {
        return Files.walk(directory)
            .filter(Files::isRegularFile)
            .filter(path -> path.getFileName().toString().endsWith(suffix))
            .collect(Collectors.toSet());
    }
    
    /**
     * Recurses through the given directory and finds all files (not directories).
     * 
     * @param directory The directory to search files in.
     * 
     * @return The set of all files in that directory.
     * 
     * @throws IOException If searching through the directory fails.
     */
    public static Set<Path> findAllFiles(Path directory) throws IOException {
        return Files.walk(directory)
                .filter(Files::isRegularFile)
                .collect(Collectors.toSet());
    }
    
    /**
     * Reads the file-size of the given file.
     * 
     * @param file The file to get the size for. Must be a file and not a directory.
     * 
     * @return The size of the file in bytes.
     * 
     * @throws IOException If reading the file-size fails.
     */
    public static long getFileSize(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new NoSuchFileException(file + " is not a file");
        }
        
        Map<String, Object> attributes = Files.readAttributes(file, "size", LinkOption.NOFOLLOW_LINKS);
        
        return (long) attributes.get("size");
    }
    
    /**
     * Rigs file operations to throw {@link IOException}s. Supported methods:
     * <ul>
     *  <li>{@link #newInputStream(Path)} returns a stream that throws on read operations</li>
     *  <li>{@link #deleteFile(Path)} always throws</li>
     * </ul>
     * This method should only be used by test cases.
     * 
     * @param fileReadingShouldFail Whether all newly created file readers should throw {@link IOException}s.
     */
    static void setRigFileOperationsToFail(boolean fileReadingShouldFail) {
        FileUtils.fileOperationsShouldFail = fileReadingShouldFail;
    }
    
    /**
     * Creates an {@link InputStream} to read the contents of the given file. This should be preferred over
     * directly creating a {@link FileInputStream} as this method allows test cases to force {@link IOException}s
     * for testing purposes.
     * 
     * @param file The file to read.
     * 
     * @return An {@link InputStream} for the file's content.
     * 
     * @throws IOException If the file does not exist, is a directory rather than a regular file, or for 
     *         some other reason cannot be opened for reading.
     */
    public static InputStream newInputStream(Path file) throws IOException {
        InputStream result;
        if (!fileOperationsShouldFail) {
            result = Files.newInputStream(file);
        } else {
            result = new InputStream() {
                
                @Override
                public int read() throws IOException {
                    throw new IOException("Rigged to fail");
                }
            };
        }
        return result;
    }
    
    /**
     * Deletes a directory with all content of it.
     * 
     * @param directory The folder to delete.
     * 
     * @throws IOException If deleting the directory fails.
     */
    public static void deleteDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IOException(directory + " is not a directory");
        }
        
        if (fileOperationsShouldFail) {
            throw new IOException("rigged to fail");
        }
        
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
}
