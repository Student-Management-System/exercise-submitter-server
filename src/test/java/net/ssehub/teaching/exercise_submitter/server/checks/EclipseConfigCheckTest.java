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
package net.ssehub.teaching.exercise_submitter.server.checks;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.server.checks.ResultMessage.MessageType;

public class EclipseConfigCheckTest {

    private static final File TESTDATA = new File("src/test/resources/EclipseConfigCheckTest");
    
    @Test
    @DisplayName("does not succeed if .classpath file is missing")
    public void missingClasspath() {
        File direcory = new File(TESTDATA, "missingClasspath");
        assertThat("Precondition: directory with test files should exist",
                direcory.isDirectory(), is(true));
        
        EclipseConfigCheck check = new EclipseConfigCheck();
        
        boolean success = check.run(direcory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create an error message", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("eclipse-configuration", MessageType.ERROR, "Does not contain a valid eclipse project")
                )))
        );
    }
    
    @Test
    @DisplayName("does not succeed if .project file is missing")
    public void missingProject() {
        File direcory = new File(TESTDATA, "missingProject");
        assertThat("Precondition: directory with test files should exist",
                direcory.isDirectory(), is(true));
        
        EclipseConfigCheck check = new EclipseConfigCheck();
        
        boolean success = check.run(direcory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create an error message", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("eclipse-configuration", MessageType.ERROR, "Does not contain a valid eclipse project")
                )))
        );
    }
    
    @Test
    @DisplayName("does not succeed if .project file has an invalid format")
    public void invalidProjectFIle() {
        File direcory = new File(TESTDATA, "invalidProject");
        assertThat("Precondition: directory with test files should exist",
                direcory.isDirectory(), is(true));
        
        EclipseConfigCheck check = new EclipseConfigCheck();
        
        boolean success = check.run(direcory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create an error message", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("eclipse-configuration", MessageType.ERROR, "Does not contain a valid eclipse project")
                )))
        );
    }
    
    @Test
    @DisplayName("succeeds on valid project")
    public void valid() {
        File direcory = new File(TESTDATA, "javaProject");
        assertThat("Precondition: directory with test files should exist",
                direcory.isDirectory(), is(true));
        
        EclipseConfigCheck check = new EclipseConfigCheck();
        
        boolean success = check.run(direcory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should create no error messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("succeeds on non-Java project if Java projects are not required")
    public void nonJavaProjectAllowed() {
        File direcory = new File(TESTDATA, "nonJavaProject");
        assertThat("Precondition: directory with test files should exist",
                direcory.isDirectory(), is(true));
        
        EclipseConfigCheck check = new EclipseConfigCheck();
        
        boolean success = check.run(direcory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should create no error messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("does not succeed on non-Java project if Java projects are required")
    public void nonJavaProjectNotAllowed() {
        File direcory = new File(TESTDATA, "nonJavaProject");
        assertThat("Precondition: directory with test files should exist",
                direcory.isDirectory(), is(true));
        
        EclipseConfigCheck check = new EclipseConfigCheck();
        check.setRequireJavaProject(true);
        
        boolean success = check.run(direcory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create an error message", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("eclipse-configuration", MessageType.ERROR, "Submission is not a Java project")
                        .setFile(new File(".project"))
                )))
        );
    }
    
    @Test
    @DisplayName("succeeds on valid Java project if Java projects are required")
    public void validRequiredJavaProject() {
        File direcory = new File(TESTDATA, "javaProject");
        assertThat("Precondition: directory with test files should exist",
                direcory.isDirectory(), is(true));
        
        EclipseConfigCheck check = new EclipseConfigCheck();
        check.setRequireJavaProject(true);
        
        boolean success = check.run(direcory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should create no error messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("succeeds on non-Checkstyle project if Checkstyle projects are not required")
    public void nonCheckstyleProjectAllowed() {
        File direcory = new File(TESTDATA, "javaProject");
        assertThat("Precondition: directory with test files should exist",
                direcory.isDirectory(), is(true));
        
        EclipseConfigCheck check = new EclipseConfigCheck();
        
        boolean success = check.run(direcory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should create no error messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("warns for non-Checkstyle project if Checkstyle projects are required")
    public void nonCheckstyleProjectNotAllowed() {
        File direcory = new File(TESTDATA, "javaProject");
        assertThat("Precondition: directory with test files should exist",
                direcory.isDirectory(), is(true));
        
        EclipseConfigCheck check = new EclipseConfigCheck();
        check.setRequireCheckstyleProject(true);
        
        boolean success = check.run(direcory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should create a warning message", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("eclipse-configuration", MessageType.WARNING, "Submission does not have Checkstyle enabled")
                        .setFile(new File(".project"))
                )))
        );
    }
    
    @Test
    @DisplayName("succeeds on valid Checkstyle project if Checkstyle projects are required")
    public void validRequiredCheckstyleProject() {
        File direcory = new File(TESTDATA, "checkstyleProject");
        assertThat("Precondition: directory with test files should exist",
                direcory.isDirectory(), is(true));
        
        EclipseConfigCheck check = new EclipseConfigCheck();
        check.setRequireCheckstyleProject(true);
        
        boolean success = check.run(direcory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should create no error messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("creates internal error message if reading the .project file fails")
    public void unreadableProjectFile() throws IOException {
        File direcory = new File(TESTDATA, "javaProject");
        assertThat("Precondition: directory with test files should exist",
                direcory.isDirectory(), is(true));
        
        File projectFile = new File(direcory, ".project");
        assertThat("Precondition: project file should exist",
                projectFile.isFile(), is(true));
        try {
            EclipseConfigCheck check = new EclipseConfigCheck();
           
            FileUtilsTest.setRigFileOperationsToFail(true);
            
            boolean success = check.run(direcory);
            
            assertAll(
                () -> assertThat("Postcondition: should not succeed", success, is(false)),
                () -> assertThat("Postcondition: should create an error message", check.getResultMessages(), is(Arrays.asList(
                        new ResultMessage("eclipse-configuration", MessageType.ERROR, "An internal error occurred while checking eclipse project")
                    )))
            );
            
        } finally {
            FileUtilsTest.setRigFileOperationsToFail(false);
        }
    }
    
    @Test
    @DisplayName("getters return previously set values")
    public void getters() {
        EclipseConfigCheck check = new EclipseConfigCheck();
        
        assertAll("should return default values",
            () -> assertThat("default value for requires Java projects", check.getRequireJavaProject(), is(false)),
            () -> assertThat("default value for requires Checkstyle projects", check.getRequireCheckstyleProject(), is(false))
        );
        
        check.setRequireJavaProject(true);
        assertAll("should return configured values",
            () -> assertThat("value for requires Java projects", check.getRequireJavaProject(), is(true)),
            () -> assertThat("value for requires Checkstyle projects", check.getRequireCheckstyleProject(), is(false))
        );
        
        check.setRequireJavaProject(false);
        check.setRequireCheckstyleProject(true);
        assertAll("should return configured values",
            () -> assertThat("value for requires Java projects", check.getRequireJavaProject(), is(false)),
            () -> assertThat("value for requires Checkstyle projects", check.getRequireCheckstyleProject(), is(true))
        );
    }
    
    @BeforeAll
    public static void initLogger() {
        LoggingSetup.setupStdoutLogging();
    }
    
}
