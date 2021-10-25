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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class FileUtilsTest {
    
    private static final File TESTDATA = new File("src/test/resources/FileUtilsTest");
    
    private static final File TEMP_DIRECTORY = new File(TESTDATA, "temp_directory");

    public static void setRigFileOperationsToFail(boolean fileReadingShouldFail) {
        FileUtils.setRigFileOperationsToFail(fileReadingShouldFail);
    }
    
    @Test
    public void findSuffixFilesEmptyDirectory() {
        File directory = new File(TESTDATA, "emptyDirectory");
        assertThat("Precondition: test directory (" + directory.getPath() + ") should exist",
                directory.isDirectory());
        
        assertThat("Postcondition: should not find files in empty directory",
                FileUtils.findFilesBySuffix(directory, ".txt"), is(new HashSet<>(Arrays.asList())));
        
        assertThat("Postcondition: should not find files in empty directory",
                FileUtils.findFilesBySuffix(directory, ""), is(new HashSet<>(Arrays.asList())));
    }
    
    @Test
    public void findSuffixFilesNoMatches() {
        File directory = new File(TESTDATA, "noTxt");
        assertThat("Precondition: test directory (" + directory.getPath() + ") should exist",
                directory.isDirectory());
        
        assertThat("Postcondition: should not find any matching files",
                FileUtils.findFilesBySuffix(directory, ".txt"), is(new HashSet<>(Arrays.asList())));
    }
    
    @Test
    public void findSuffixFilesOneMatch() {
        File directory = new File(TESTDATA, "oneTxt");
        assertThat("Precondition: test directory (" + directory.getPath() + ") should exist",
                directory.isDirectory());
        
        assertThat("Postcondition: should find one match",
                FileUtils.findFilesBySuffix(directory, ".txt"), is(new HashSet<>(Arrays.asList(
                        new File(directory, "file.txt")))));
    }
    
    @Test
    public void findSuffixFilesMultipleMatch() {
        File directory = new File(TESTDATA, "multipleTxt");
        assertThat("Precondition: test directory (" + directory.getPath() + ") should exist",
                directory.isDirectory());
        
        assertThat("Postcondition: should find two matches",
                FileUtils.findFilesBySuffix(directory, ".txt"), is(new HashSet<>(Arrays.asList(
                        new File(directory, "file.txt"),  new File(directory, "another.txt")))));
    }
    
    @Test
    public void findSuffixFilesNoDotSuffix() {
        File directory = new File(TESTDATA, "noTxt");
        assertThat("Precondition: test directory (" + directory.getPath() + ") should exist",
                directory.isDirectory());
        
        assertThat("Postcondition: should find one matching file",
                FileUtils.findFilesBySuffix(directory, "ffix"), is(new HashSet<>(Arrays.asList(
                        new File(directory, "noSuffix")))));
    }
    
    @Test
    public void findSuffixFilesSubdirectories() {
        File directory = new File(TESTDATA, "subDirectories");
        assertThat("Precondition: test directory (" + directory.getPath() + ") should exist",
                directory.isDirectory());
        
        assertThat("Postcondition: should find multiple matching files in sub-directories",
                FileUtils.findFilesBySuffix(directory, ".txt"), is(new HashSet<>(Arrays.asList(
                        new File(directory, "subdir1/file.txt"),
                        new File(directory, "subdir2/another.txt"),
                        new File(directory, "subdir2/subsubdir/file.txt")))));
    }
    
    @Test
    public void findAllFilesEmptyDirectory() {
        File directory = new File(TESTDATA, "emptyDirectory");
        assertThat("Precondition: test directory (" + directory.getPath() + ") should exist",
                directory.isDirectory());
        
        assertThat("Postcondition: should not find files in empty directory",
                FileUtils.findAllFiles(directory), is(new HashSet<>(Arrays.asList())));
    }
    
    @Test
    public void findAllFilesFlat() {
        File directory = new File(TESTDATA, "oneTxt");
        assertThat("Precondition: test directory (" + directory.getPath() + ") should exist",
                directory.isDirectory());
        
        assertThat("Postcondition: should find all files in flat directory",
                FileUtils.findAllFiles(directory), is(new HashSet<>(Arrays.asList(
                        new File(directory, "Compiled.class"),
                        new File(directory, "file.txt"),
                        new File(directory, "noSuffix"),
                        new File(directory, "Source.java")
                ))));
    }
    
    @Test
    public void findAllFilesNested() {
        File directory = new File(TESTDATA, "subDirectories");
        assertThat("Precondition: test directory (" + directory.getPath() + ") should exist",
                directory.isDirectory());
        
        assertThat("Postcondition: should find all files in flat directory",
                FileUtils.findAllFiles(directory), is(new HashSet<>(Arrays.asList(
                        new File(directory, "subdir1/Compiled.class"),
                        new File(directory, "subdir1/file.txt"),
                        new File(directory, "subdir1/noSuffix"),
                        new File(directory, "subdir1/Source.java"),
                        
                        new File(directory, "subdir2/Compiled.class"),
                        new File(directory, "subdir2/another.txt"),
                        new File(directory, "subdir2/noSuffix"),
                        new File(directory, "subdir2/Source.java"),
                        
                        new File(directory, "subdir2/subsubdir/Compiled.class"),
                        new File(directory, "subdir2/subsubdir/file.txt"),
                        new File(directory, "subdir2/subsubdir/noSuffix"),
                        new File(directory, "subdir2/subsubdir/Source.java")
                ))));
    }
    
    @Test
    public void relativizeDirectlyNested() {
        assertThat("Postcondition: should relativize file directly nested in base direcotry",
                FileUtils.getRelativeFile(new File("some/dir/"), new File("some/dir/file.txt")),
                is(new File("file.txt")));
    }
    
    @Test
    public void relativizeNestedInSubDirectories() {
        assertThat("Postcondition: should relativize file nested in sub-directories",
                FileUtils.getRelativeFile(new File("some/dir/"), new File("some/dir/a/b/file.txt")),
                is(new File("a/b/file.txt")));
    }
    
    @Test
    public void relativizeNextToEachOther() {
        assertThat("Postcondition: should relativize file next to directory",
                FileUtils.getRelativeFile(new File("some/dir/"), new File("some/file.txt")),
                is(new File("../file.txt")));
    }
    
    @Test
    public void relativizeFurtherOutside() {
        assertThat("Postcondition: should relativize file that is not inside directory",
                FileUtils.getRelativeFile(new File("some/dir/"), new File("another/file.txt")),
                is(new File("../../another/file.txt")));
    }
    
    @Test
    public void relativizeAbsolutePaths() {
        assertThat("Postcondition: should relativize files given as absolute paths",
                FileUtils.getRelativeFile(new File("/some/dir/"), new File("/some/dir/nested/file.txt")),
                is(new File("nested/file.txt")));
    }
    
    @Test
    public void relativizeThrowsIfDirAbsoluteAndFileRelative() {
        assertThrows(IllegalArgumentException.class, () -> {
            FileUtils.getRelativeFile(new File("/some/dir/"), new File("some/dir/nested/file.txt"));
        });
    }
    
    @Test
    public void relativizeThrowsIfDirRelativeAndFileAbsolute() {
        assertThrows(IllegalArgumentException.class, () -> {
            FileUtils.getRelativeFile(new File("some/dir/"), new File("/some/dir/nested/file.txt"));
        });
    }
    
    @Test
    public void fileSizeOnDirectory() throws IOException {
        assertThat("Precondition: test directory should exist",
                TESTDATA.isDirectory(), is(true));
        
        assertThrows(FileNotFoundException.class, () -> {
            FileUtils.getFileSize(TESTDATA);
        });
    }
    
    @Test
    public void fileSizeOnNotExistingFile() throws IOException {
        File file = new File(TESTDATA, "doesnt_exist");
        assertThat("Precondition: test file should not exist",
                file.exists(), is(false));
        
        assertThrows(FileNotFoundException.class, () -> {
            FileUtils.getFileSize(file);
        });
    }
    
    @Test
    public void fileSize100Bytes() throws IOException {
        File file = new File(TESTDATA, "100bytes.txt");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        assertThat("Postcondition: file size should be correct",
                FileUtils.getFileSize(file), is(100L));
    }
    
    @Test
    public void fileSize0Bytes() throws IOException {
        File file = new File(TESTDATA, "0bytes.txt");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        assertThat("Postcondition: file size should be correct",
                FileUtils.getFileSize(file), is(0L));
    }
    
    @Test
    public void fileSize2006Bytes() throws IOException {
        File file = new File(TESTDATA, "2006bytes.txt");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        assertThat("Postcondition: file size should be correct",
                FileUtils.getFileSize(file), is(2006L));
    }
    
    @Test
    public void newInputStreamReadsFile() throws IOException {
        File file = new File(TESTDATA, "textFile.txt");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        try (InputStream in = FileUtils.newInputStream(file)) {
            assertThat("Postcondition: should read exact file content",
                    new String(in.readAllBytes()), is("Hello World!\n"));
        }
    }
    
    @Test
    public void newInputStreamRigged() throws IOException {
        File file = new File(TESTDATA, "textFile.txt");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
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
    public void deleteFileNotExisting() throws IOException {
        File file = new File(TESTDATA, "doesnt_exit");
        assertThat("Precondition: test file should not exist",
                file.exists(), is(false));
        
        assertThrows(IOException.class, () -> {
            FileUtils.deleteFile(file);
        });
    }
    
    @Test
    public void deleteFile() throws IOException {
        File file = File.createTempFile("test-file", null);
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        FileUtils.deleteFile(file);
        
        assertThat("Postcondition: file should have been deleted",
                file.exists(), is(false));
    }
    
    @Test
    public void deleteFileRigged() throws IOException {
        File file = File.createTempFile("test-file", null);
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        try {
            setRigFileOperationsToFail(true);
            
            FileUtils.deleteFile(file);
            fail("should have thrown an IOException");
            
        } catch (IOException e) {
            assertThat("Postcondition: exception should have correct message",
                    e.getMessage(), is("Rigged to fail"));
            
        } finally {
            setRigFileOperationsToFail(false);
            
            file.delete();
            assertThat("Cleanup: file should have been deleted",
                    file.exists(), is(false));
        }
    }
    
    @Test
    public void deleteFileNonEmptyDirectory() throws IOException {
        File directory = new File(TESTDATA, "singleFile");
        assertThat("Precondition: test directory should exist",
                directory.isDirectory(), is(true));
        
        assertThat("Precondition: test directory should not be empty",
                directory.listFiles().length, not(is(0)));
        
        assertThrows(IOException.class, () -> {
            FileUtils.deleteFile(directory);
        });
    }
    
    @Test
    public void deleteFileDirectory() throws IOException {
        File file = File.createTempFile("test-file", null);
        file.delete();
        file.mkdir();
        assertThat("Precondition: test directory should exist",
                file.isDirectory(), is(true));
        assertThat("Precondition: test directory should be empty",
                file.listFiles().length, is(0));
        
        FileUtils.deleteFile(file);
        
        assertThat("Postcondition: directory should have been deleted",
                file.exists(), is(false));
    }
    
    @Test
    public void newReaderReadsFile() throws IOException {
        File file = new File(TESTDATA, "textFile.txt");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        try (Reader in = FileUtils.newReader(file)) {
            char[] buf = new char["Hello World!\n".length()];
            
            assertThat("should read full file contet",
                    in.read(buf), is(buf.length));
            
            assertThat("Postcondition: should read exact file content",
                    new String(buf), is("Hello World!\n"));
            
            assertThat("Postcondition: should have reached end of file",
                    in.read(), is(-1));
        }
    }
    
    @Test
    public void newReaderRigged() throws IOException {
        File file = new File(TESTDATA, "textFile.txt");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        try {
            setRigFileOperationsToFail(true);
            
            Reader in = FileUtils.newReader(file);
            
            try {
                in.read();
                fail("Read should throw an IOException");
            } catch (IOException e) {
                assertThat("Postcondition: should have correct error message",
                        e.getMessage(), is("Rigged to fail"));
            }
            
            in.close();
            
        } finally {
            setRigFileOperationsToFail(false);
        }
    }
    
    @Test
    public void parseXmlNonExistingFile() throws IOException, SAXException {
        File file = new File(TESTDATA, "doesnt_exist.xml");
        assertThat("Precondition: test file should not exist",
                file.exists(), is(false));
        
        assertThrows(FileNotFoundException.class, () -> {
            FileUtils.parseXml(file);
        });
    }
    
    @Test
    public void parseInvalidXml() throws IOException, SAXException {
        File file = new File(TESTDATA, "invalid.xml");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        assertThrows(SAXException.class, () -> {
            FileUtils.parseXml(file);
        });
    }
    
    @Test
    public void parseValidXml() throws IOException, SAXException {
        File file = new File(TESTDATA, "valid.xml");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        Document document = FileUtils.parseXml(file);
        
        Node parentNode = document.getDocumentElement();
        assertThat(parentNode.getNodeName(), is("parent"));
        assertThat(parentNode.getNodeType(), is(Node.ELEMENT_NODE));
        assertThat(parentNode.getChildNodes().getLength(), is(1));
        
        Node childNode = parentNode.getChildNodes().item(0);
        assertThat(childNode.getNodeName(), is("child"));
        assertThat(childNode.getNodeType(), is(Node.ELEMENT_NODE));
        assertThat(childNode.getChildNodes().getLength(), is(1));
        
        Node textNode = childNode.getChildNodes().item(0); 
        assertThat(textNode.getNodeType(), is(Node.TEXT_NODE));
        assertThat(textNode.getTextContent(), is("text"));
    }
    
    @Test
    public void parseXmlIoException() throws IOException, SAXException {
        File file = new File(TESTDATA, "valid.xml");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        FileUtilsTest.setRigFileOperationsToFail(true);
        
        assertThrows(IOException.class, () -> {
            FileUtils.parseXml(file);
        });
        
        FileUtilsTest.setRigFileOperationsToFail(false);
    }
    
    @Test
    public void deleteNonExistingDirecotry() throws IOException {
        File file = new File(TESTDATA, "doesnt_exist");
        assertThat("Precondition: test file should not exist",
                file.exists(), is(false));
        
        assertThrows(IOException.class, () -> {
            FileUtils.deleteDirectory(file);
        });
    }
    
    @Test
    public void deleteDirectoryOnFile() throws IOException {
        File file = new File(TESTDATA, "100bytes.txt");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        assertThrows(IOException.class, () -> {
            FileUtils.deleteDirectory(file);
        });
    }
    
    @Test
    public void deleteDirectoryEmpty() throws IOException {
        assertThat("Precondition: directory should exist",
                TEMP_DIRECTORY.isDirectory(), is(true));
        assertThat("Precondition: directory should be empty",
                TEMP_DIRECTORY.listFiles().length, is(0));
        
        FileUtils.deleteDirectory(TEMP_DIRECTORY);
        
        assertThat("Postcondition: directory should not exist",
                TEMP_DIRECTORY.exists(), is(false));
    }
    
    @Test
    public void deleteDirectorySingleFileInside() throws IOException {
        assertThat("Precondition: directory should exist",
                TEMP_DIRECTORY.isDirectory(), is(true));
        assertThat("Precondition: directory should be empty",
                TEMP_DIRECTORY.listFiles().length, is(0));
        
        File file = new File(TEMP_DIRECTORY, "some_file.txt");
        file.createNewFile();
        
        assertThat("Precondition: directory should not be empty",
                TEMP_DIRECTORY.listFiles().length, is(1));
        
        FileUtils.deleteDirectory(TEMP_DIRECTORY);
        
        assertThat("Postcondition: directory should not exist",
                TEMP_DIRECTORY.exists(), is(false));
        assertThat("Postcondition: file should not exist",
                file.exists(), is(false));
    }
    
    @Test
    public void deleteDirectoryMultipleFilesInside() throws IOException {
        assertThat("Precondition: directory should exist",
                TEMP_DIRECTORY.isDirectory(), is(true));
        assertThat("Precondition: directory should be empty",
                TEMP_DIRECTORY.listFiles().length, is(0));
        
        for (int i = 0; i < 10; i++) {
            File file = new File(TEMP_DIRECTORY, "some_file" + i + ".txt");
            file.createNewFile();
        }
        
        assertThat("Precondition: directory should not be empty",
                TEMP_DIRECTORY.listFiles().length, is(10));
        
        FileUtils.deleteDirectory(TEMP_DIRECTORY);
        
        assertThat("Postcondition: directory should not exist",
                TEMP_DIRECTORY.exists(), is(false));
    }
    
    @Test
    public void deleteDirectoryCantDeleteFile() throws IOException {
        File directory = new File(TESTDATA, "singleFile");
        assertThat("Precondition: directory should exist",
                directory.isDirectory(), is(true));
        assertThat("Precondition: directory should not be empty",
                directory.listFiles().length, is(1));
        
        File file = new File(directory, "some_file.txt");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        setRigFileOperationsToFail(true);
        
        assertThrows(IOException.class, () -> {
            FileUtils.deleteDirectory(directory);
        });
        
        setRigFileOperationsToFail(false);
    }
    
    @Test
    public void deleteDirectoryNestedDirectories() throws IOException {
        assertThat("Precondition: directory should exist",
                TEMP_DIRECTORY.isDirectory(), is(true));
        assertThat("Precondition: directory should be empty",
                TEMP_DIRECTORY.listFiles().length, is(0));
        
        File nested = new File(TEMP_DIRECTORY, "nested");
        nested.mkdir();
        assertThat("Precondition: nested directory should exist",
                TEMP_DIRECTORY.isDirectory(), is(true));
        
        for (int i = 0; i < 10; i++) {
            File file = new File(nested, "some_file" + i + ".txt");
            file.createNewFile();
        }
        
        for (int i = 0; i < 10; i++) {
            File file = new File(TEMP_DIRECTORY, "some_file" + i + ".txt");
            file.createNewFile();
        }
        
        assertThat("Precondition: directory should not be empty",
                TEMP_DIRECTORY.listFiles().length, is(11));
        
        assertThat("Precondition: nested directory should not be empty",
                nested.listFiles().length, is(10));
        
        FileUtils.deleteDirectory(TEMP_DIRECTORY);
        
        assertThat("Postcondition: directory should not exist",
                TEMP_DIRECTORY.exists(), is(false));
        assertThat("Postcondition: nested directory should not exist",
                nested.exists(), is(false));
    }
    
    @Test
    public void temporaryDirectoryCleaned() throws IOException {
        File tempdir = FileUtils.createTemporaryDirectory();
        
        assertThat("created temporary directory should exist",
                tempdir.isDirectory(), is(true));
        assertThat("created temporary directory should be empty",
                tempdir.listFiles().length, is(0));
        
        FileUtils.cleanTemporaryFolders();
        
        assertThat("created temporary directory should not exist after cleaning",
                tempdir.exists(), is(false));
    }
    
    @Test
    public void temporaryDirectoryCleanFails() throws IOException {
        File tempdir = FileUtils.createTemporaryDirectory();
        
        assertThat("created temporary directory should exist",
                tempdir.isDirectory(), is(true));
        assertThat("created temporary directory should be empty",
                tempdir.listFiles().length, is(0));
        
        try {
            setRigFileOperationsToFail(true);
            
            FileUtils.cleanTemporaryFolders();
            
            assertThat("created temporary directory should still exist after failed cleaning",
                    tempdir.isDirectory(), is(true));
        } finally {
            setRigFileOperationsToFail(false);
        }
        
        FileUtils.deleteDirectory(tempdir);
        assertThat("cleanup: created temporary directory should not exist after cleanup",
                tempdir.exists(), is(false));
    }
    
    @Test
    public void temporaryDirectoryCreateFile() throws IOException {
        File tempdir = FileUtils.createTemporaryDirectory();
        
        assertThat("precondition: created temporary directory should exist",
                tempdir.isDirectory(), is(true));
        assertThat("precondition: created temporary directory should be empty",
                tempdir.listFiles().length, is(0));
        
        File nested = new File(tempdir, "nested_file");
        
        assertThat("precondition: nested file should not exist",
                nested.exists(), is(false));
        
        assertThat("postcondition: creation of nested file should succeed",
                nested.createNewFile(), is(true));
        assertThat("postcondition: nested file should exist",
                nested.isFile(), is(true));
    }
    
    @BeforeEach
    public void createTempDirectory() {
        if (!TEMP_DIRECTORY.isDirectory()) {
            boolean created = TEMP_DIRECTORY.mkdir();
            if (!created) {
                fail("Setup: Could not create empty temporary test directory " + TEMP_DIRECTORY.getPath());
            }
        }
    }
    
    @AfterEach
    public void clearTempDirectory() throws IOException {
        if (TEMP_DIRECTORY.isDirectory()) {
            Files.walkFileTree(TEMP_DIRECTORY.toPath(), new SimpleFileVisitor<Path>() {
                
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
        File directory = new File(TESTDATA, "emptyDirectory");
        if (!directory.isDirectory()) {
            boolean created = directory.mkdir();
            if (!created) {
                fail("Setup: Could not create empty test directory " + directory.getPath());
            }
        }
    }
    
    @BeforeAll
    public static void initLogger() {
        LoggingSetup.setupStdoutLogging();
    }
    
}
