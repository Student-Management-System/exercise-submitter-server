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
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

public class FileSizeCheckTest {
    
    private static final File TESTDATA = new File("src/test/resources/FileSizeCheckTest");
    
    @Test
    @DisplayName("succeeds on empty directory")
    public void emptyDirectory() {
        File directory = new File(TESTDATA, "emptyDirectory");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        FileSizeCheck check = new FileSizeCheck();
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should create no messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("succeeds on single file that holds the file-size limit")
    public void sinlgeFileLimitHeld() {
        File directory = new File(TESTDATA, "singleFile100Bytes");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        FileSizeCheck check = new FileSizeCheck();
        check.setMaxFileSize(200);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should create no messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("succeeds on single file that holds the file-size limit exactly")
    public void sinlgeFileLimitHeldExactly() {
        File directory = new File(TESTDATA, "singleFile100Bytes");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        FileSizeCheck check = new FileSizeCheck();
        check.setMaxFileSize(100);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should create no messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("does not succeed on single file that breaks the file-size limit")
    public void sinlgeFileLimitViolated() {
        File directory = new File(TESTDATA, "singleFile100Bytes");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        FileSizeCheck check = new FileSizeCheck();
        check.setMaxFileSize(99);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create an error message", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("file-size", MessageType.ERROR, "File is too large").setFile(new File("100bytes.txt"))
                )))
        );
    }
    
    @Test
    @DisplayName("does not succeed with a single file that is larger than the submission-size limit")
    public void submissionSizeLimitViolatedBySingleFile() {
        File directory = new File(TESTDATA, "singleFile100Bytes");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        FileSizeCheck check = new FileSizeCheck();
        check.setMaxSubmissionSize(99);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create an error message", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("file-size", MessageType.ERROR, "Submission size is too large")
                )))
        );
    }
    
    @Test
    @DisplayName("succeeds on multiple files that hold the file-size limit")
    public void multipleFileLimitsHeld() {
        File directory = new File(TESTDATA, "multipleFiles");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        FileSizeCheck check = new FileSizeCheck();
        check.setMaxFileSize(200);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should create no messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("does not succeed with multiple files breaking the file-size limit")
    public void multipleFileLimitsViolated() {
        File directory = new File(TESTDATA, "multipleFiles");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        FileSizeCheck check = new FileSizeCheck();
        check.setMaxFileSize(99);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create a error messages", check.getResultMessages(), containsInAnyOrder(
                    new ResultMessage("file-size", MessageType.ERROR, "File is too large").setFile(new File("100bytes.txt")),
                    new ResultMessage("file-size", MessageType.ERROR, "File is too large").setFile(new File("200bytes.txt"))
                ))
        );
    }
    
    @Test
    @DisplayName("does not succeed with some of the submitted files breaking the file-size limit")
    public void multipleFileLimitsSomeViolated() {
        File directory = new File(TESTDATA, "multipleFiles");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        FileSizeCheck check = new FileSizeCheck();
        check.setMaxFileSize(150);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create an error message", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("file-size", MessageType.ERROR, "File is too large").setFile(new File("200bytes.txt"))
                )))
        );
    }
    
    @Test
    @DisplayName("does not succeed with multiple files adding up to more than the submission-size limit")
    public void multipleFilesSubmissionTooLarge() {
        File directory = new File(TESTDATA, "multipleFiles");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        FileSizeCheck check = new FileSizeCheck();
        check.setMaxFileSize(200);
        check.setMaxSubmissionSize(250);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create an error message", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("file-size", MessageType.ERROR, "Submission size is too large")
                )))
        );
    }
    
    @Test
    @DisplayName("succeeds with multiple files adding up to exactly the submission-size limit")
    public void multipleFilesSubmissionHeldExactly() {
        File directory = new File(TESTDATA, "multipleFiles");
        assertThat("Precondition: directory with test files should exist",
                directory.isDirectory(), is(true));
        
        FileSizeCheck check = new FileSizeCheck();
        check.setMaxFileSize(200);
        check.setMaxSubmissionSize(300);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should create no error messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("getters return previously set values")
    public void getters() {
        FileSizeCheck check = new FileSizeCheck();
        
        assertAll("should return default values",
            () -> assertThat(check.getMaxFileSize(), is(10485760L)),
            () -> assertThat(check.getMaxSubmissionSize(), is(10485760L))
        );
        
        check.setMaxFileSize(123);
        check.setMaxSubmissionSize(456);
        
        assertAll(
            () -> assertThat(check.getMaxFileSize(), is(123L)),
            () -> assertThat(check.getMaxSubmissionSize(), is(456L))
        );
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
    
}
