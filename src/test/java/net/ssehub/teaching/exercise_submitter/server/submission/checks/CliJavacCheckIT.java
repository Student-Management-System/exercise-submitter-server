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

import java.nio.file.Files;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

public class CliJavacCheckIT extends JavacCheckIT {

    @Override
    protected CliJavacCheck creatInstance() {
        return new CliJavacCheck();
    }
    
    @Test
    @DisplayName("creates internal error for invalid javac command")
    public void invalidJavacCommand() {
        testDirectory = TESTDATA;
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        CliJavacCheck check = creatInstance();
        check.setJavacCommand("doesnt_exist");
        
        boolean success = check.run(testDirectory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should contain result messages about internal error", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("javac", MessageType.ERROR, "An internal error occurred while running javac")
                )))
        );
    }
    
    @Test
    @DisplayName("setter and getter for javac command work")
    public void setJavacCommand() {
        CliJavacCheck check = new CliJavacCheck();
        
        assertThat("should have default value", check.getJavacCommand(), is("javac"));
        
        check.setJavacCommand("/opt/some/javac");
        
        assertThat("should have explicitly set value", check.getJavacCommand(), is("/opt/some/javac"));
    }
    
    /*
     * Add pitest-ignore tag to some test cases from our parent class
     */
    
    @Test
    @Tag("pitest-ignore")
    @Override
    public void submissionCheckClassesNotInClasspath() {
        super.submissionCheckClassesNotInClasspath();
    }
    
    @Test
    @Tag("pitest-ignore")
    @Override
    public void classpathMissingLibrary() {
        super.classpathMissingLibrary();
    }
    
}
