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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FileUtilsTest {
    
    private static final Path TESTDATA = Path.of("src/test/resources/FileUtilsTest");
    
    private static final Path TEMP_DIRECTORY = TESTDATA.resolve("temp_directory");

    public static void setRigFileOperationsToFail(boolean fileReadingShouldFail) {
        FileUtils.setRigFileOperationsToFail(fileReadingShouldFail);
    }
    
    @Test
    public void findSuffixFilesEmptyDirectory() throws IOException {
        Path directory = TESTDATA.resolve("emptyDirectory");
        assertThat("Precondition: test directory (" + directory + ") should exist",
                Files.isDirectory(directory));
        
        assertThat("Postcondition: should not find files in empty directory",
                FileUtils.findFilesBySuffix(directory, ".txt"), is(new HashSet<>(Arrays.asList())));
        
        assertThat("Postcondition: should not find files in empty directory",
                FileUtils.findFilesBySuffix(directory, ""), is(new HashSet<>(Arrays.asList())));
    }
    
    @Test
    public void findSuffixFilesNoMatches() throws IOException {
        Path directory = TESTDATA.resolve("noTxt");
        assertThat("Precondition: test directory (" + directory + ") should exist",
                Files.isDirectory(directory));
        
        assertThat("Postcondition: should not find any matching files",
                FileUtils.findFilesBySuffix(directory, ".txt"), is(new HashSet<>(Arrays.asList())));
    }
    
    @Test
    public void findSuffixFilesOneMatch() throws IOException {
        Path directory = TESTDATA.resolve("oneTxt");
        assertThat("Precondition: test directory (" + directory + ") should exist",
                Files.isDirectory(directory));
        
        assertThat("Postcondition: should find one match",
                FileUtils.findFilesBySuffix(directory, ".txt"), is(new HashSet<>(Arrays.asList(
                        directory.resolve("file.txt")))));
    }
    
    @Test
    public void findSuffixFilesMultipleMatch() throws IOException {
        Path directory = TESTDATA.resolve("multipleTxt");
        assertThat("Precondition: test directory (" + directory + ") should exist",
                Files.isDirectory(directory));
        
        assertThat("Postcondition: should find two matches",
                FileUtils.findFilesBySuffix(directory, ".txt"), is(new HashSet<>(Arrays.asList(
                        directory.resolve("file.txt"),  directory.resolve("another.txt")))));
    }
    
    @Test
    public void findSuffixFilesNoDotSuffix() throws IOException {
        Path directory = TESTDATA.resolve("noTxt");
        assertThat("Precondition: test directory (" + directory + ") should exist",
                Files.isDirectory(directory));
        
        assertThat("Postcondition: should find one matching file",
                FileUtils.findFilesBySuffix(directory, "ffix"), is(new HashSet<>(Arrays.asList(
                        directory.resolve("noSuffix")))));
    }
    
    @Test
    public void findSuffixFilesSubdirectories() throws IOException {
        Path directory = TESTDATA.resolve("subDirectories");
        assertThat("Precondition: test directory (" + directory + ") should exist",
                Files.isDirectory(directory));
        
        assertThat("Postcondition: should find multiple matching files in sub-directories",
                FileUtils.findFilesBySuffix(directory, ".txt"), is(new HashSet<>(Arrays.asList(
                        directory.resolve("subdir1/file.txt"),
                        directory.resolve("subdir2/another.txt"),
                        directory.resolve("subdir2/subsubdir/file.txt")))));
    }
    
    @Test
    public void findAllFilesEmptyDirectory() throws IOException {
        Path directory = TESTDATA.resolve("emptyDirectory");
        assertThat("Precondition: test directory (" + directory + ") should exist",
                Files.isDirectory(directory));
        
        assertThat("Postcondition: should not find files in empty directory",
                FileUtils.findAllFiles(directory), is(new HashSet<>(Arrays.asList())));
    }
    
    @Test
    public void findAllFilesFlat() throws IOException {
        Path directory = TESTDATA.resolve("oneTxt");
        assertThat("Precondition: test directory (" + directory + ") should exist",
                Files.isDirectory(directory));
        
        assertThat("Postcondition: should find all files in flat directory",
                FileUtils.findAllFiles(directory), is(new HashSet<>(Arrays.asList(
                        directory.resolve("Compiled.class"),
                        directory.resolve("file.txt"),
                        directory.resolve("noSuffix"),
                        directory.resolve("Source.java")
                ))));
    }
    
    @Test
    public void findAllFilesNested() throws IOException {
        Path directory = TESTDATA.resolve("subDirectories");
        assertThat("Precondition: test directory (" + directory + ") should exist",
                Files.isDirectory(directory));
        
        assertThat("Postcondition: should find all files in flat directory",
                FileUtils.findAllFiles(directory), is(new HashSet<>(Arrays.asList(
                        directory.resolve("subdir1/Compiled.class"),
                        directory.resolve("subdir1/file.txt"),
                        directory.resolve("subdir1/noSuffix"),
                        directory.resolve("subdir1/Source.java"),
                        
                        directory.resolve("subdir2/Compiled.class"),
                        directory.resolve("subdir2/another.txt"),
                        directory.resolve("subdir2/noSuffix"),
                        directory.resolve("subdir2/Source.java"),
                        
                        directory.resolve("subdir2/subsubdir/Compiled.class"),
                        directory.resolve("subdir2/subsubdir/file.txt"),
                        directory.resolve("subdir2/subsubdir/noSuffix"),
                        directory.resolve("subdir2/subsubdir/Source.java")
                ))));
    }
    
    @Test
    public void fileSizeOnDirectory() throws IOException {
        assertThat("Precondition: test directory should exist",
                Files.isDirectory(TESTDATA), is(true));
        
        assertThrows(NoSuchFileException.class, () -> {
            FileUtils.getFileSize(TESTDATA);
        });
    }
    
    @Test
    public void fileSizeOnNotExistingFile() throws IOException {
        Path file = TESTDATA.resolve("doesnt_exist");
        assertThat("Precondition: test file should not exist",
                Files.exists(file), is(false));
        
        assertThrows(NoSuchFileException.class, () -> {
            FileUtils.getFileSize(file);
        });
    }
    
    @Test
    public void fileSize100Bytes() throws IOException {
        Path file = TESTDATA.resolve("100bytes.txt");
        assertThat("Precondition: test file should exist",
                Files.isRegularFile(file), is(true));
        
        assertThat("Postcondition: file size should be correct",
                FileUtils.getFileSize(file), is(100L));
    }
    
    @Test
    public void fileSize0Bytes() throws IOException {
        Path file = TESTDATA.resolve("0bytes.txt");
        assertThat("Precondition: test file should exist",
                Files.isRegularFile(file), is(true));
        
        assertThat("Postcondition: file size should be correct",
                FileUtils.getFileSize(file), is(0L));
    }
    
    @Test
    public void fileSize2006Bytes() throws IOException {
        Path file = TESTDATA.resolve("2006bytes.txt");
        assertThat("Precondition: test file should exist",
                Files.isRegularFile(file), is(true));
        
        assertThat("Postcondition: file size should be correct",
                FileUtils.getFileSize(file), is(2006L));
    }
    
    @Test
    public void newInputStreamReadsFile() throws IOException {
        Path file = TESTDATA.resolve("textFile.txt");
        assertThat("Precondition: test file should exist",
                Files.isRegularFile(file), is(true));
        
        try (InputStream in = FileUtils.newInputStream(file)) {
            assertThat("Postcondition: should read exact file content",
                    new String(in.readAllBytes()), is("Hello World!\n"));
        }
    }
    
    @Test
    public void newInputStreamRigged() throws IOException {
        Path file = TESTDATA.resolve("textFile.txt");
        assertThat("Precondition: test file should exist",
                Files.isRegularFile(file), is(true));
        
        try {
            setRigFileOperationsToFail(true);
            
            InputStream in = FileUtils.newInputStream(file);
            
            try {
                in.read();
                fail("Read should throw an IOException");
            } catch (IOException e) {
                assertThat("Postcondition: should have correct error message",
                        e.getMessage(), is("Rigged to fail"));
            }
            
        } finally {
            setRigFileOperationsToFail(false);
        }
    }
    
    @Test
    public void deleteNonExistingDirecotry() throws IOException {
        Path file = TESTDATA.resolve("doesnt_exist");
        assertThat("Precondition: test file should not exist",
                Files.exists(file), is(false));
        
        assertThrows(IOException.class, () -> {
            FileUtils.deleteDirectory(file);
        });
    }
    
    @Test
    public void deleteDirectoryOnFile() throws IOException {
        Path file = TESTDATA.resolve("100bytes.txt");
        assertThat("Precondition: test file should exist",
                Files.isRegularFile(file), is(true));
        
        assertThrows(IOException.class, () -> {
            FileUtils.deleteDirectory(file);
        });
    }
    
    @Test
    public void deleteDirectoryEmpty() throws IOException {
        assertThat("Precondition: directory should exist",
                Files.isDirectory(TEMP_DIRECTORY), is(true));
        assertThat("Precondition: directory should be empty",
                Files.list(TEMP_DIRECTORY).count(), is(0L));
        
        FileUtils.deleteDirectory(TEMP_DIRECTORY);
        
        assertThat("Postcondition: directory should not exist",
                Files.exists(TEMP_DIRECTORY), is(false));
    }
    
    @Test
    public void deleteDirectorySingleFileInside() throws IOException {
        assertThat("Precondition: directory should exist",
                Files.isDirectory(TEMP_DIRECTORY), is(true));
        assertThat("Precondition: directory should be empty",
                Files.list(TEMP_DIRECTORY).count(), is(0L));
        
        Path file = TEMP_DIRECTORY.resolve("some_file.txt");
        Files.createFile(file);
        
        assertThat("Precondition: directory should not be empty",
                Files.list(TEMP_DIRECTORY).count(), is(1L));
        
        FileUtils.deleteDirectory(TEMP_DIRECTORY);
        
        assertThat("Postcondition: directory should not exist",
                Files.exists(TEMP_DIRECTORY), is(false));
        assertThat("Postcondition: file should not exist",
                Files.exists(file), is(false));
    }
    
    @Test
    public void deleteDirectoryMultipleFilesInside() throws IOException {
        assertThat("Precondition: directory should exist",
                Files.isDirectory(TEMP_DIRECTORY), is(true));
        assertThat("Precondition: directory should be empty",
                Files.list(TEMP_DIRECTORY).count(), is(0L));
        
        for (int i = 0; i < 10; i++) {
            Path file = TEMP_DIRECTORY.resolve("some_file" + i + ".txt");
            Files.createFile(file);
        }
        
        assertThat("Precondition: directory should not be empty",
                Files.list(TEMP_DIRECTORY).count(), is(10L));
        
        FileUtils.deleteDirectory(TEMP_DIRECTORY);
        
        assertThat("Postcondition: directory should not exist",
                Files.exists(TEMP_DIRECTORY), is(false));
    }
    
    @Test
    public void deleteDirectoryCantDeleteFile() throws IOException {
        Path directory = TESTDATA.resolve("singleFile");
        assertThat("Precondition: directory should exist",
                Files.isDirectory(directory), is(true));
        assertThat("Precondition: directory should not be empty",
                Files.list(directory).count(), is(1L));
        
        Path file = directory.resolve("some_file.txt");
        assertThat("Precondition: test file should exist",
                Files.isRegularFile(file), is(true));
        
        setRigFileOperationsToFail(true);
        
        assertThrows(IOException.class, () -> {
            FileUtils.deleteDirectory(directory);
        });
        
        setRigFileOperationsToFail(false);
    }
    
    @Test
    public void deleteDirectoryNestedDirectories() throws IOException {
        assertThat("Precondition: directory should exist",
                Files.isDirectory(TEMP_DIRECTORY), is(true));
        assertThat("Precondition: directory should be empty",
                Files.list(TEMP_DIRECTORY).count(), is(0L));
        
        Path nested = TEMP_DIRECTORY.resolve("nested");
        Files.createDirectory(nested);
        assertThat("Precondition: nested directory should exist",
                Files.isDirectory(TEMP_DIRECTORY), is(true));
        
        for (int i = 0; i < 10; i++) {
            Path file = nested.resolve("some_file" + i + ".txt");
            Files.createFile(file);
        }
        
        for (int i = 0; i < 10; i++) {
            Path file = TEMP_DIRECTORY.resolve("some_file" + i + ".txt");
            Files.createFile(file);
        }
        
        assertThat("Precondition: directory should not be empty",
                Files.list(TEMP_DIRECTORY).count(), is(11L));
        
        assertThat("Precondition: nested directory should not be empty",
                Files.list(nested).count(), is(10L));
        
        FileUtils.deleteDirectory(TEMP_DIRECTORY);
        
        assertThat("Postcondition: directory should not exist",
                Files.exists(TEMP_DIRECTORY), is(false));
        assertThat("Postcondition: nested directory should not exist",
                Files.exists(nested), is(false));
    }
    
    @BeforeEach
    public void createTempDirectory() {
        if (!Files.isDirectory(TEMP_DIRECTORY)) {
            try {
                Files.createDirectory(TEMP_DIRECTORY);
            } catch (IOException e) {
                e.printStackTrace();
                fail("Setup: Could not create empty temporary test directory " + TEMP_DIRECTORY);
            }
        }
    }
    
    @AfterEach
    public void clearTempDirectory() throws IOException {
        if (Files.isDirectory(TEMP_DIRECTORY)) {
            Files.walkFileTree(TEMP_DIRECTORY, new SimpleFileVisitor<Path>() {
                
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                
            });
        }
    }
    
    @BeforeAll
    public static void createEmptyDirectory() {
        Path directory = TESTDATA.resolve("emptyDirectory");
        if (!Files.isDirectory(directory)) {
            try {
                Files.createDirectory(directory);
            } catch (IOException e) {
                e.printStackTrace();
                fail("Setup: Could not create empty test directory " + directory);
            }
        }
    }
    
}
