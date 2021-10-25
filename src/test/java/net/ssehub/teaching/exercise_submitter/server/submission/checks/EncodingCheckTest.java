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
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

public class EncodingCheckTest {

    private static final File TESTDATA = new File("src/test/resources/EncodingCheckTest");
    
    @Test
    @DisplayName("succeeds on submission with no files")
    public void noFiles() {
        File directory = new File(TESTDATA, "emptyDirectory");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        EncodingCheck check = new EncodingCheck();
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should create no messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("succeeds on UTF-8 files with UTF-8 required")
    public void expectUtf8OnUtf8() {
        File directory = new File(TESTDATA, "UTF-8");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        EncodingCheck check = new EncodingCheck();
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should create no messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("does not succeed on windows-1258 files with UTF-8 required")
    public void expectUtf8OnWindows1258() {
        File directory = new File(TESTDATA, "windows-1258");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        EncodingCheck check = new EncodingCheck();
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create an error message", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("encoding", MessageType.ERROR, "File has invalid encoding; expected UTF-8").setFile(new File("umlauts.txt"))
                )))
        );
    }
    
    @Test
    @DisplayName("does not succeed on UTF-16 files with UTF-8 required")
    public void expectUtf8OnUtf16() {
        File directory = new File(TESTDATA, "UTF-16");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        EncodingCheck check = new EncodingCheck();
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create an error message", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("encoding", MessageType.ERROR, "File has invalid encoding; expected UTF-8").setFile(new File("umlauts.txt"))
                )))
        );
    }
    
    @Test
    @DisplayName("does not succeed on ISO 8859-1 files with UTF-8 expected")
    public void expectUtf8OnIso88591() {
        File directory = new File(TESTDATA, "ISO 8859-1");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        EncodingCheck check = new EncodingCheck();
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create an error message", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("encoding", MessageType.ERROR, "File has invalid encoding; expected UTF-8").setFile(new File("umlauts.txt"))
                )))
        );
    }
    
    @Test
    @DisplayName("creates an internal error for an unreadable file")
    public void unreadableFile() throws IOException {
        File directory = new File(TESTDATA, "UTF-8");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        File file = new File(directory, "umlauts.txt");
        assertThat("Precondition: file should exist",
                file.isFile(), is(true));
        try {
            
            EncodingCheck check = new EncodingCheck();
            
            FileUtilsTest.setRigFileOperationsToFail(true);
            
            boolean success = check.run(directory);
            
            assertAll(
                () -> assertThat("Postcondition: should not succeed", success, is(false)),
                () -> assertThat("Postcondition: should create an internal error message", check.getResultMessages(), is(Arrays.asList(
                        new ResultMessage("encoding", MessageType.ERROR, "An internal error occurred while checking file encoding")
                    )))
            );
            
        } finally {
            FileUtilsTest.setRigFileOperationsToFail(false);
        }
    }
    
    @Test
    @DisplayName("ignores binary files with a known mime-type (pdf and application/octet-stream)")
    public void ignoreBinaryFileKnownMimetype() {
        File directory = new File(TESTDATA, "binaryFile");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        EncodingCheck check = new EncodingCheck();
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should create no error messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("ignores files with an unknown mime-type (empty file)")
    public void ignoreBinaryFileNoMimetype() {
        File directory = new File(TESTDATA, "emptyFile");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        EncodingCheck check = new EncodingCheck();
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should create no error messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("getter returns previously set value")
    public void getter() {
        EncodingCheck check = new EncodingCheck();
        assertThat("should return default value",
                check.getWantedCharset(), is(StandardCharsets.UTF_8));
        
        check.setWantedCharset(StandardCharsets.ISO_8859_1);
        assertThat(check.getWantedCharset(), is(StandardCharsets.ISO_8859_1));
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
