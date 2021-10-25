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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Static utility methods for files and directories.
 * 
 * @author Adam
 */
public class FileUtils {

    private static final Logger LOGGER = Logger.getLogger(FileUtils.class.getName());
    
    private static boolean shutdownHookRegistered;
    
    private static Set<File> temporaryDirectories = new HashSet<>();
    
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
     */
    public static Set<File> findFilesBySuffix(File directory, String suffix) {
        Set<File> result = new HashSet<>();
        findeFilesBySuffixImpl(directory, suffix, result);
        return result;
    }
    
    /**
     * Recurses through the given directory and finds all files (not directories) which name ends with the given suffix.
     * 
     * @param directory The directory to search files in.
     * @param suffix The suffix to search for.
     * @param result The result set to add all found matching files to. 
     */
    private static void findeFilesBySuffixImpl(File directory, String suffix, Set<File> result) {
        for (File nested : directory.listFiles()) {
            if (nested.isDirectory()) {
                findeFilesBySuffixImpl(nested, suffix, result);
                
            } else if (nested.getName().endsWith(suffix)) {
                result.add(nested);
            }
        }
    }
    
    /**
     * Recurses through the given directory and finds all files (not directories).
     * 
     * @param directory The directory to search files in.
     * 
     * @return The set of all files in that directory.
     */
    public static Set<File> findAllFiles(File directory) {
        Set<File> result = new HashSet<>();
        findAllFilesImpl(directory, result);
        return result;
    }
    
    /**
     * Recurses through the given directory and finds all files (not directories).
     * 
     * @param directory The directory to search files in.
     * @param result The result set to add all files to.
     */
    public static void findAllFilesImpl(File directory, Set<File> result) {
        for (File nested : directory.listFiles()) {
            if (nested.isDirectory()) {
                findAllFilesImpl(nested, result);
                
            } else {
                result.add(nested);
            }
        }
    }
    
    /**
     * Creates a relative file path from a given directory. Basically, this strips the directory prefix from the given
     * file path.
     * 
     * @param directory The directory considered as the base for the relative result.
     * @param file The file path to make relative.
     * 
     * @return The file as a relative path based on <code>directory</code>.
     * 
     * @throws IllegalArgumentException If one of the parameters is relative and the other is absolute.
     */
    public static File getRelativeFile(File directory, File file) throws IllegalArgumentException {
        return directory.toPath().relativize(file.toPath()).toFile();
    }
    
    /**
     * Reads the file-size of the given file.
     * 
     * @param file The file to get the size for. Must be a file ({@link File#isFile()} and not a directory.
     * 
     * @return The size of the file in bytes.
     * 
     * @throws IOException If reading the file-size fails.
     */
    public static long getFileSize(File file) throws IOException {
        if (!file.isFile()) {
            throw new FileNotFoundException(file + " is not a file");
        }
        
        Map<String, Object> attributes = Files.readAttributes(
                file.toPath(), "size", LinkOption.NOFOLLOW_LINKS);
        
        return (long) attributes.get("size");
    }
    
    /**
     * Rigs file operations to throw {@link IOException}s. Supported methods:
     * <ul>
     *  <li>{@link #newInputStream(File)} returns a stream that throws on read operations</li>
     *  <li>{@link #newReader(File)} returns a reader that throws on read operations</li>
     *  <li>{@link #deleteFile(File)} always throws</li>
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
     * @throws FileNotFoundException If the file does not exist, is a directory rather than a regular file, or for 
     *         some other reason cannot be opened for reading.
     */
    public static InputStream newInputStream(File file) throws FileNotFoundException {
        InputStream result;
        if (!fileOperationsShouldFail) {
            result = new FileInputStream(file);
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
     * Creates a {@link Reader} to read the contents of the given file. Uses UTF-8 as the charset. This should
     * be preferred over directly creating a {@link FileReader} as this method allows test cases to force
     * {@link IOException}s for testing purposes.
     * 
     * @param file The file to read.
     * 
     * @return An {@link Reader} for the file's content.
     * 
     * @throws IOException If the file does not exist, is a directory rather than a regular file, or for 
     *         some other reason cannot be opened for reading.
     */
    public static Reader newReader(File file) throws IOException {
        Reader result;
        if (!fileOperationsShouldFail) {
            result = new FileReader(file, StandardCharsets.UTF_8);
        } else {
            result = new Reader() {
                
                @Override
                public int read(char[] cbuf, int off, int len) throws IOException {
                    throw new IOException("Rigged to fail");
                }
                
                @Override
                public void close() throws IOException {}
                
            };
        }
        return result;
    }
    
    /**
     * Deletes a single file or an empty directory.
     * 
     * @param file The file to delete.
     * 
     * @throws IOException If the given file is not a file or deletion fails.
     */
    public static void deleteFile(File file) throws IOException {
        if (fileOperationsShouldFail) {
            throw new IOException("Rigged to fail");
        }
        
        Files.delete(file.toPath());
    }
    
    /**
     * Parses the XML content of the given file.
     * 
     * @param file The file to parse.
     * 
     * @return The XML {@link Document}.
     * 
     * @throws IOException If reading the file fails.
     * @throws SAXException If the XML parsing failed.
     */
    public static Document parseXml(File file) throws IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
        DocumentBuilder parser;
        try {
            parser = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e); // cannot happen
        }
        parser.setErrorHandler(null);
        Document result = parser.parse(newInputStream(file));
        result.normalize();
        return result;
    }

    /**
     * Deletes a directory with all content of it.
     * 
     * @param directory The folder to delete.
     * 
     * @throws IOException If deleting the directory fails.
     */
    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.isDirectory()) {
            throw new IOException(directory + " is not a directory");
        }
        
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else {
                deleteFile(file);
            }
        }
        
        deleteFile(directory);
    }
    
    /**
     * Creates a temporary folder that will be deleted by a shutdown hook.
     * 
     * @return The temporary folder.
     * 
     * @throws IOException If creating the temporary folder fails.
     * 
     * @see Runtime#addShutdownHook(Thread)
     */
    public static File createTemporaryDirectory() throws IOException {
        if (!shutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(FileUtils::cleanTemporaryFolders));
            shutdownHookRegistered = true;
        }
        
        File temporaryFile = File.createTempFile("submission-check", null);
        deleteFile(temporaryFile);
        
        if (!temporaryFile.mkdir()) {
            throw new IOException("Could not create temporary directory");
        }
        
        synchronized (temporaryDirectories) {
            temporaryDirectories.add(temporaryFile);
        }
        
        return temporaryFile;
    }
    
    /**
     * Removes all temporary folders created via {@link #createTemporaryDirectory()}. Called by a shutdown hook.
     * Package visibility for test cases.
     */
    static void cleanTemporaryFolders() {
        synchronized (temporaryDirectories) {
            for (File directory : temporaryDirectories) {
                try {
                    deleteDirectory(directory);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Could not delete temporary directory " + directory, e);
                }
            }
            temporaryDirectories.clear();
        }
    }
    
}
