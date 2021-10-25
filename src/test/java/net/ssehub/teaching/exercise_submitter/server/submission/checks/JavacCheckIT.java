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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

public abstract class JavacCheckIT {
    
    protected static final File TESTDATA = new File("src/test/resources/JavacCheckTest");

    protected File testDirecotry;
    
    protected abstract JavacCheck creatInstance();
    
    @Test
    public void noJavaFiles() {
        testDirecotry = new File(TESTDATA, "noJavaFiles");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run with no Java files should fail",
                check.run(testDirecotry), is(false));
        
        assertThat("Postcondition: should contain one ResultMessage for no Java files",
                check.getResultMessages(), is(Arrays.asList(
                        new ResultMessage("javac", MessageType.ERROR, "No Java files found")
                )));
    }
    
    @Test
    public void testResultMessageClearing() {
        testDirecotry = new File(TESTDATA, "noJavaFiles");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        assertThat("Precondition: getResultMessages() should be empty before first invocation",
                check.getResultMessages(), is(Arrays.asList()));
        
        assertThat("Postcondition: run with no Java files should fail",
                check.run(testDirecotry), is(false));
        
        assertThat("Postcondition: should contain one ResultMessage for no Java files",
                check.getResultMessages(), is(Arrays.asList(
                        new ResultMessage("javac", MessageType.ERROR, "No Java files found"
                ))));
        
        assertThat("Postcondition: should clear getResultMessages() after first invocation",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void singleCompilingFile() {
        testDirecotry = new File(TESTDATA, "singleCompilingFile");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run with single correct file should succeed",
                check.run(testDirecotry), is(true));
        
        assertThat("Postcondition: should not have any ResultMessages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void singleIncorrectFile() {
        testDirecotry = new File(TESTDATA, "singleIncorrectFile");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run with single incorrect file should not succeed",
                check.run(testDirecotry), is(false));
        
        assertThat("Postcondition: should contain a result message for the compilation error",
                check.getResultMessages(), is(Arrays.asList(
                        new ResultMessage("javac", MessageType.ERROR, "';' expected")
                            .setFile(new File("HelloWorld.java")).setLine(4).setColumn(43)
                )));
    }
    
    @Test
    public void singleIncorrectFileMultipleErrors() {
        testDirecotry = new File(TESTDATA, "singleIncorrectFileMultipleErrors");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run with single incorrect file with multiple errors should not succeed",
                check.run(testDirecotry), is(false));
        
        assertThat("Postcondition: should contain one ResultMessage for each compilation error",
                check.getResultMessages(), containsInAnyOrder(
                        new ResultMessage("javac", MessageType.ERROR, "';' expected")
                            .setFile(new File("HelloWorld.java")).setLine(4).setColumn(43),
                        new ResultMessage("javac", MessageType.ERROR, "reached end of file while parsing")
                            .setFile(new File("HelloWorld.java")).setLine(5).setColumn(6)
                ));
    }
    
    @Test
    public void multipleCompilingFiles() {
        testDirecotry = new File(TESTDATA, "multipleCompilingFiles");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run with multiple correct files should succeed",
                check.run(testDirecotry), is(true));
        
        assertThat("Postcondition: should not have any ResultMessages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void multipleIncorrectFiles() {
        testDirecotry = new File(TESTDATA, "multipleIncorrectFiles");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run with multiple incorrect files should not succeed",
                check.run(testDirecotry), is(false));
        
        assertThat("Postcondition: should contain a result message for the compilation error",
                check.getResultMessages(), containsInAnyOrder(
                        new ResultMessage("javac", MessageType.ERROR, "';' expected")
                            .setFile(new File("Main.java")).setLine(4).setColumn(37),
                        new ResultMessage("javac", MessageType.ERROR, "invalid method declaration; return type required")
                            .setFile(new File("Util.java")).setLine(3).setColumn(12)
                ));
    }
    
    @Test
    public void multipleWithOneIncorrectFiles() {
        testDirecotry = new File(TESTDATA, "multipleWithOneIncorrectFiles");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run with multiple files with one incorrect should not succeed",
                check.run(testDirecotry), is(false));
        
        assertThat("Postcondition: should contain a result message for the compilation error",
                check.getResultMessages(), is(Arrays.asList(
                        new ResultMessage("javac", MessageType.ERROR, "non-static method method() cannot be referenced from a static context")
                            .setFile(new File("Main.java")).setLine(5).setColumn(13)
                )));
    }
    
    @Test
    public void correctPackages() {
        testDirecotry = new File(TESTDATA, "packagesCorrect");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run without compilation error should succeed",
                check.run(testDirecotry), is(true));
        
        assertThat("Postcondition: should not have any ResultMessages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void compilerErrorsInPackages() {
        testDirecotry = new File(TESTDATA, "packagesIncorrect");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run with compilation error should not succeed",
                check.run(testDirecotry), is(false));
        
        assertThat("Postcondition: should contain result messages with correct path for each compilation error",
                check.getResultMessages(), containsInAnyOrder(
                        new ResultMessage("javac", MessageType.ERROR, "';' expected")
                            .setFile(new File("main/Main.java")).setLine(8).setColumn(37),
                        new ResultMessage("javac", MessageType.ERROR, "not a statement")
                            .setFile(new File("util/Util.java")).setLine(6).setColumn(19)
                ));
    }
    
    @Test
    public void invalidJavaVersion() {
        testDirecotry = TESTDATA;
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        check.setJavaVersion(3); // only supported >= 6
        
        assertThat("Postcondition: run with invalid parameter should not succeed",
                check.run(testDirecotry), is(false));
        
        String message = "An internal error occurred while running javac";
        if (check instanceof CliJavacCheck) {
            message = "javac failed without message";
        }
        
        assertThat("Postcondition: should contain result messages about internal error",
                check.getResultMessages(), is(Arrays.asList(
                        new ResultMessage("javac", MessageType.ERROR, message)
                )));
    }
    
    @Test
    public void warningsEnabled() {
        testDirecotry = new File(TESTDATA, "warnings");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        check.setEnableWarnings(true);
        
        assertThat("Postcondition: run with compilation warnings should succeed",
                check.run(testDirecotry), is(true));
        
        String message1 = "division by zero";
        String message2 = "stop() in java.lang.Thread has been deprecated";
        if (check instanceof CliJavacCheck) {
            message1 = "[divzero] " + message1;
            message2 = "[deprecation] stop() in Thread has been deprecated";
        }
        
        assertThat("Postcondition: should contain result messages for each compilation warning",
                check.getResultMessages(), containsInAnyOrder(
                        new ResultMessage("javac", MessageType.WARNING, message1)
                            .setFile(new File("HelloWorld.java")).setLine(4).setColumn(21),
                        new ResultMessage("javac", MessageType.WARNING, message2)
                            .setFile(new File("HelloWorld.java")).setLine(5).setColumn(21)
                ));
    }
    
    @Test
    public void warningsDisabled() {
        testDirecotry = new File(TESTDATA, "warnings");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        
        assertThat("Postcondition: run with compilation warnings disabled should succeed",
                check.run(testDirecotry), is(true));
        
        assertThat("Postcondition: should contain no result messages with warnings disabled",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void version8FeaturesCorrect() {
        testDirecotry = new File(TESTDATA, "version8Features");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        check.setJavaVersion(8);
        
        assertThat("Postcondition: run with high-enough Java version should succeed",
                check.run(testDirecotry), is(true));
        
        assertThat("Postcondition: should contain no result messages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void version8FeaturesIncorrect() {
        testDirecotry = new File(TESTDATA, "version8Features");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        check.setJavaVersion(7);
        
        assertThat("Postcondition: run with not high-enough Java version should not succeed",
                check.run(testDirecotry), is(false));
        
        assertThat("Postcondition: should contain no result messages",
                check.getResultMessages(), is(Arrays.asList(
                        new ResultMessage("javac", MessageType.ERROR, "lambda expressions are not supported in -source 7")
                            .setFile(new File("Lambda.java")).setLine(6).setColumn(48)
                )));
    }
    
    @Test
    public void charsetUtf8UmlautsCorrect() {
        testDirecotry = new File(TESTDATA, "umlauts");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        
        assertThat("Postcondition: run with correct encoding set should succeed",
                check.run(testDirecotry), is(true));
        
        assertThat("Postcondition: should contain no result messages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void charsetIso88591UmlautsIncorrect() {
        testDirecotry = new File(TESTDATA, "umlauts_ISO-8859-1");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        
        assertThat("Postcondition: run with incorrect encoding should not succeed",
                check.run(testDirecotry), is(false));
        
        String message = "illegal character: '\\ufffd'";
        if (check instanceof CliJavacCheck) {
            message = "unmappable character (0xE4) for encoding UTF-8";
        }
        
        assertThat("Postcondition: should contain no result messages",
                check.getResultMessages(), containsInAnyOrder(
                        new ResultMessage("javac", MessageType.ERROR, message)
                            .setFile(new File("Umlauts.java")).setLine(4).setColumn(13),
                        new ResultMessage("javac", MessageType.ERROR, message)
                            .setFile(new File("Umlauts.java")).setLine(5).setColumn(28),
                        new ResultMessage("javac", MessageType.ERROR, "not a statement")
                            .setFile(new File("Umlauts.java")).setLine(4).setColumn(9)
                ));
    }
    
    @Test
    public void charsetIso88591UmlautsCorrect() {
        testDirecotry = new File(TESTDATA, "umlauts_ISO-8859-1");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        check.setCharset(StandardCharsets.ISO_8859_1);
        
        assertThat("Postcondition: run with correct encoding set should succeed",
                check.run(testDirecotry), is(true));
        
        assertThat("Postcondition: should contain no result messages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void srcFolderWithPackages() {
        testDirecotry = new File(TESTDATA, "srcFolder");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        
        assertThat("Postcondition: run with files in the src folder should succeed",
                check.run(testDirecotry), is(true));
        
        assertThat("Postcondition: should create no error messages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void srcFolderWithClasspathSet() {
        testDirecotry = new File(TESTDATA, "srcFolder");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        check.addToClasspath(new File(testDirecotry, "src"));
        
        assertThat("Postcondition: run with files in the src folder should succeed",
                check.run(testDirecotry), is(true));
        
        assertThat("Postcondition: should create no error messages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void classpathMissingLibrary() {
        testDirecotry = new File(TESTDATA, "library");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        
        assertThat("Postcondition: run with invalid classpath set should not succeed",
                check.run(testDirecotry), is(false));
        
        List<ResultMessage> expected = new LinkedList<>();
        expected.add(new ResultMessage("javac", MessageType.ERROR, "package util does not exist")
                .setFile(new File("Main.java")).setLine(1).setColumn(12));
        expected.add(new ResultMessage("javac", MessageType.ERROR, "cannot find symbol")
                    .setFile(new File("Main.java")).setLine(7).setColumn(9));
        
        assertThat("Postcondition: should create error messages",
                check.getResultMessages(), containsInAnyOrder(expected.toArray()));
    }
    
    @Test
    public void classpathLibraryCorrect() {
        testDirecotry = new File(TESTDATA, "library");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        File library = new File(testDirecotry, "lib/util-lib.jar");
        assertThat("Precondition: library does not exist",
                library.isFile());
        
        JavacCheck check = creatInstance();
        check.addToClasspath(library);
        
        assertThat("Postcondition: run with correct classpath set should succeed",
                check.run(testDirecotry), is(true));
        
        assertThat("Postcondition: should create no error messages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void classpathCleared() {
        JavacCheck check = creatInstance();
        
        testDirecotry = new File(TESTDATA, "library");
        File library = new File(testDirecotry, "lib/util-lib.jar");
        
        check.addToClasspath(library);
        
        assertThat("Precondition: classpath is added",
                check.getClasspath(), is(Arrays.asList(library)));
        
        check.clearClasspath();
        
        assertThat("Postcondition: classpath is cleared",
                check.getClasspath(), is(Arrays.asList()));
    }
    
    @Test
    public void submissionCheckClassesNotInClasspath() {
        testDirecotry = new File(TESTDATA, "usesClassesFromSubmissionCheck");
        assertThat("Precondition: directory with test files does not exist",
                testDirecotry.isDirectory());
        
        JavacCheck check = creatInstance();
        
        assertThat("Postcondition: file that uses submission check classes should not compile",
                check.run(testDirecotry), is(false));
        
        assertThat("Postcondition: should create error messages",
                check.getResultMessages(), containsInAnyOrder(
                        new ResultMessage("javac", MessageType.ERROR, "package net.ssehub.teaching.submission_check does not exist")
                                .setFile(new File("Main.java")).setLine(4).setColumn(45),
                        new ResultMessage("javac", MessageType.ERROR, "package net.ssehub.teaching.submission_check does not exist")
                                .setFile(new File("Main.java")).setLine(5).setColumn(59)
                ));
    }
    
    @Test
    public void getters() {
        JavacCheck check = creatInstance();
        
        assertThat("should return correct default value",
                check.getJavaVersion(), is(11));
        
        assertThat("should return correct default value",
                check.getCharset(), is(StandardCharsets.UTF_8));
        
        assertThat("should return correct default value",
                check.getEnableWarnings(), is(false));
        
        assertThat("should return correct default value",
                check.getClasspath(), is(Arrays.asList()));
        
        check.setJavaVersion(8);
        assertThat(check.getJavaVersion(), is(8));
        
        check.setCharset(StandardCharsets.ISO_8859_1);
        assertThat(check.getCharset(), is(StandardCharsets.ISO_8859_1));
        
        check.setEnableWarnings(true);
        assertThat(check.getEnableWarnings(), is(true));
        
        check.addToClasspath(new File("abc"));
        assertThat(check.getClasspath(), is(Arrays.asList(new File("abc"))));
        
        check.addToClasspath(new File("def"));
        assertThat(check.getClasspath(), is(Arrays.asList(new File("abc"), new File("def"))));
    }
    
    // TODO: create a test for an error or warning with a line number but without a column
    
    // TODO: create a test for an error with a file but without a line number
    
    @BeforeAll
    public static void checkJavacInstallation() throws InterruptedException, IOException {
        ProcessBuilder command = new ProcessBuilder("javac", "-version");
        command.redirectError(Redirect.DISCARD);
        command.redirectOutput(Redirect.DISCARD);
        
        int exitStatus = command.start().waitFor();
        
        assertThat("Precondition: javac command doesn't execute",
                exitStatus, is(0));
    }
    
    @AfterEach
    public void cleanup() {
        if (testDirecotry != null && testDirecotry.isDirectory()) {
            removeClassFiles(testDirecotry);
        }
    }
    
    private void removeClassFiles(File directory) {
        for (File nested : directory.listFiles()) {
            if (nested.isFile() && nested.getName().endsWith(".class")) {
                boolean deleted = nested.delete();
                if (!deleted) {
                    fail("Cleanup: Failed to delete class file " + nested);
                }
                
            } else if (nested.isDirectory()) {
                removeClassFiles(nested);
            }
        }
    }
    
}
