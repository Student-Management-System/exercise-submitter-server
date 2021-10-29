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

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

public abstract class JavacCheckIT {
    
    protected static final Path TESTDATA = Path.of("src/test/resources/JavacCheckTest");

    protected Path testDirectory;
    
    protected abstract JavacCheck creatInstance();
    
    @Test
    public void noJavaFiles() {
        testDirectory = TESTDATA.resolve("noJavaFiles");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run with no Java files should fail",
                check.run(testDirectory), is(false));
        
        assertThat("Postcondition: should contain one ResultMessage for no Java files",
                check.getResultMessages(), is(Arrays.asList(
                        new ResultMessage("javac", MessageType.ERROR, "No Java files found")
                )));
    }
    
    @Test
    public void testResultMessageClearing() {
        testDirectory = TESTDATA.resolve("noJavaFiles");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        assertThat("Precondition: getResultMessages() should be empty before first invocation",
                check.getResultMessages(), is(Arrays.asList()));
        
        assertThat("Postcondition: run with no Java files should fail",
                check.run(testDirectory), is(false));
        
        assertThat("Postcondition: should contain one ResultMessage for no Java files",
                check.getResultMessages(), is(Arrays.asList(
                        new ResultMessage("javac", MessageType.ERROR, "No Java files found"
                ))));
        
        assertThat("Postcondition: should clear getResultMessages() after first invocation",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void singleCompilingFile() {
        testDirectory = TESTDATA.resolve("singleCompilingFile");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run with single correct file should succeed",
                check.run(testDirectory), is(true));
        
        assertThat("Postcondition: should not have any ResultMessages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void singleIncorrectFile() {
        testDirectory = TESTDATA.resolve("singleIncorrectFile");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run with single incorrect file should not succeed",
                check.run(testDirectory), is(false));
        
        assertThat("Postcondition: should contain a result message for the compilation error",
                check.getResultMessages(), is(Arrays.asList(
                        new ResultMessage("javac", MessageType.ERROR, "';' expected")
                            .setFile(Path.of("HelloWorld.java")).setLine(4).setColumn(43)
                )));
    }
    
    @Test
    public void singleIncorrectFileMultipleErrors() {
        testDirectory = TESTDATA.resolve("singleIncorrectFileMultipleErrors");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run with single incorrect file with multiple errors should not succeed",
                check.run(testDirectory), is(false));
        
        assertThat("Postcondition: should contain one ResultMessage for each compilation error",
                check.getResultMessages(), containsInAnyOrder(
                        new ResultMessage("javac", MessageType.ERROR, "';' expected")
                            .setFile(Path.of("HelloWorld.java")).setLine(4).setColumn(43),
                        new ResultMessage("javac", MessageType.ERROR, "reached end of file while parsing")
                            .setFile(Path.of("HelloWorld.java")).setLine(5).setColumn(6)
                ));
    }
    
    @Test
    public void multipleCompilingFiles() {
        testDirectory = TESTDATA.resolve("multipleCompilingFiles");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run with multiple correct files should succeed",
                check.run(testDirectory), is(true));
        
        assertThat("Postcondition: should not have any ResultMessages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void multipleIncorrectFiles() {
        testDirectory = TESTDATA.resolve("multipleIncorrectFiles");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run with multiple incorrect files should not succeed",
                check.run(testDirectory), is(false));
        
        assertThat("Postcondition: should contain a result message for the compilation error",
                check.getResultMessages(), containsInAnyOrder(
                        new ResultMessage("javac", MessageType.ERROR, "';' expected")
                            .setFile(Path.of("Main.java")).setLine(4).setColumn(37),
                        new ResultMessage("javac", MessageType.ERROR, "invalid method declaration; return type required")
                            .setFile(Path.of("Util.java")).setLine(3).setColumn(12)
                ));
    }
    
    @Test
    public void multipleWithOneIncorrectFiles() {
        testDirectory = TESTDATA.resolve("multipleWithOneIncorrectFiles");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run with multiple files with one incorrect should not succeed",
                check.run(testDirectory), is(false));
        
        assertThat("Postcondition: should contain a result message for the compilation error",
                check.getResultMessages(), is(Arrays.asList(
                        new ResultMessage("javac", MessageType.ERROR, "non-static method method() cannot be referenced from a static context")
                            .setFile(Path.of("Main.java")).setLine(5).setColumn(13)
                )));
    }
    
    @Test
    public void correctPackages() {
        testDirectory = TESTDATA.resolve("packagesCorrect");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run without compilation error should succeed",
                check.run(testDirectory), is(true));
        
        assertThat("Postcondition: should not have any ResultMessages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void compilerErrorsInPackages() {
        testDirectory = TESTDATA.resolve("packagesIncorrect");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        assertThat("Postcondition: run with compilation error should not succeed",
                check.run(testDirectory), is(false));
        
        assertThat("Postcondition: should contain result messages with correct path for each compilation error",
                check.getResultMessages(), containsInAnyOrder(
                        new ResultMessage("javac", MessageType.ERROR, "';' expected")
                            .setFile(Path.of("main/Main.java")).setLine(8).setColumn(37),
                        new ResultMessage("javac", MessageType.ERROR, "not a statement")
                            .setFile(Path.of("util/Util.java")).setLine(6).setColumn(19)
                ));
    }
    
    @Test
    public void invalidJavaVersion() {
        testDirectory = TESTDATA;
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        check.setJavaVersion(3); // only supported >= 6
        
        assertThat("Postcondition: run with invalid parameter should not succeed",
                check.run(testDirectory), is(false));
        
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
        testDirectory = TESTDATA.resolve("warnings");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        check.setEnableWarnings(true);
        
        assertThat("Postcondition: run with compilation warnings should succeed",
                check.run(testDirectory), is(true));
        
        String message1 = "division by zero";
        String message2 = "stop() in java.lang.Thread has been deprecated";
        if (check instanceof CliJavacCheck) {
            message1 = "[divzero] " + message1;
            message2 = "[deprecation] stop() in Thread has been deprecated";
        }
        
        assertThat("Postcondition: should contain result messages for each compilation warning",
                check.getResultMessages(), containsInAnyOrder(
                        new ResultMessage("javac", MessageType.WARNING, message1)
                            .setFile(Path.of("HelloWorld.java")).setLine(4).setColumn(21),
                        new ResultMessage("javac", MessageType.WARNING, message2)
                            .setFile(Path.of("HelloWorld.java")).setLine(5).setColumn(21)
                ));
    }
    
    @Test
    public void warningsDisabled() {
        testDirectory = TESTDATA.resolve("warnings");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        
        assertThat("Postcondition: run with compilation warnings disabled should succeed",
                check.run(testDirectory), is(true));
        
        assertThat("Postcondition: should contain no result messages with warnings disabled",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void version8FeaturesCorrect() {
        testDirectory = TESTDATA.resolve("version8Features");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        check.setJavaVersion(8);
        
        assertThat("Postcondition: run with high-enough Java version should succeed",
                check.run(testDirectory), is(true));
        
        assertThat("Postcondition: should contain no result messages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void version8FeaturesIncorrect() {
        testDirectory = TESTDATA.resolve("version8Features");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        check.setJavaVersion(7);
        
        assertThat("Postcondition: run with not high-enough Java version should not succeed",
                check.run(testDirectory), is(false));
        
        assertThat("Postcondition: should contain no result messages",
                check.getResultMessages(), is(Arrays.asList(
                        new ResultMessage("javac", MessageType.ERROR, "lambda expressions are not supported in -source 7")
                            .setFile(Path.of("Lambda.java")).setLine(6).setColumn(48)
                )));
    }
    
    @Test
    public void charsetUtf8UmlautsCorrect() {
        testDirectory = TESTDATA.resolve("umlauts");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        
        assertThat("Postcondition: run with correct encoding set should succeed",
                check.run(testDirectory), is(true));
        
        assertThat("Postcondition: should contain no result messages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void charsetIso88591UmlautsIncorrect() {
        testDirectory = TESTDATA.resolve("umlauts_ISO-8859-1");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        
        assertThat("Postcondition: run with incorrect encoding should not succeed",
                check.run(testDirectory), is(false));
        
        String message = "illegal character: '\\ufffd'";
        if (check instanceof CliJavacCheck) {
            message = "unmappable character (0xE4) for encoding UTF-8";
        }
        
        assertThat("Postcondition: should contain no result messages",
                check.getResultMessages(), containsInAnyOrder(
                        new ResultMessage("javac", MessageType.ERROR, message)
                            .setFile(Path.of("Umlauts.java")).setLine(4).setColumn(13),
                        new ResultMessage("javac", MessageType.ERROR, message)
                            .setFile(Path.of("Umlauts.java")).setLine(5).setColumn(28),
                        new ResultMessage("javac", MessageType.ERROR, "not a statement")
                            .setFile(Path.of("Umlauts.java")).setLine(4).setColumn(9)
                ));
    }
    
    @Test
    public void charsetIso88591UmlautsCorrect() {
        testDirectory = TESTDATA.resolve("umlauts_ISO-8859-1");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        check.setCharset(StandardCharsets.ISO_8859_1);
        
        assertThat("Postcondition: run with correct encoding set should succeed",
                check.run(testDirectory), is(true));
        
        assertThat("Postcondition: should contain no result messages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void srcFolderWithPackages() {
        testDirectory = TESTDATA.resolve("srcFolder");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        
        assertThat("Postcondition: run with files in the src folder should succeed",
                check.run(testDirectory), is(true));
        
        assertThat("Postcondition: should create no error messages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void srcFolderWithClasspathSet() {
        testDirectory = TESTDATA.resolve("srcFolder");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        check.addToClasspath(testDirectory.resolve("src"));
        
        assertThat("Postcondition: run with files in the src folder should succeed",
                check.run(testDirectory), is(true));
        
        assertThat("Postcondition: should create no error messages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void classpathMissingLibrary() {
        testDirectory = TESTDATA.resolve("library");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        
        assertThat("Postcondition: run with invalid classpath set should not succeed",
                check.run(testDirectory), is(false));
        
        List<ResultMessage> expected = new LinkedList<>();
        expected.add(new ResultMessage("javac", MessageType.ERROR, "package util does not exist")
                .setFile(Path.of("Main.java")).setLine(1).setColumn(12));
        expected.add(new ResultMessage("javac", MessageType.ERROR, "cannot find symbol")
                    .setFile(Path.of("Main.java")).setLine(7).setColumn(9));
        
        assertThat("Postcondition: should create error messages",
                check.getResultMessages(), containsInAnyOrder(expected.toArray()));
    }
    
    @Test
    public void classpathLibraryCorrect() {
        testDirectory = TESTDATA.resolve("library");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        Path library = testDirectory.resolve("lib/util-lib.jar");
        assertThat("Precondition: library does not exist",
                Files.isRegularFile(library));
        
        JavacCheck check = creatInstance();
        check.addToClasspath(library);
        
        assertThat("Postcondition: run with correct classpath set should succeed",
                check.run(testDirectory), is(true));
        
        assertThat("Postcondition: should create no error messages",
                check.getResultMessages(), is(Arrays.asList()));
    }
    
    @Test
    public void classpathCleared() {
        JavacCheck check = creatInstance();
        
        testDirectory = TESTDATA.resolve("library");
        Path library = testDirectory.resolve("lib/util-lib.jar");
        
        check.addToClasspath(library);
        
        assertThat("Precondition: classpath is added",
                check.getClasspath(), is(Arrays.asList(library)));
        
        check.clearClasspath();
        
        assertThat("Postcondition: classpath is cleared",
                check.getClasspath(), is(Arrays.asList()));
    }
    
    @Test
    public void submissionCheckClassesNotInClasspath() {
        testDirectory = TESTDATA.resolve("usesClassesFromSubmissionCheck");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(testDirectory));
        
        JavacCheck check = creatInstance();
        
        assertThat("Postcondition: file that uses submission check classes should not compile",
                check.run(testDirectory), is(false));
        
        assertThat("Postcondition: should create error messages",
                check.getResultMessages(), containsInAnyOrder(
                        new ResultMessage("javac", MessageType.ERROR, "package net.ssehub.teaching.submission_check does not exist")
                                .setFile(Path.of("Main.java")).setLine(4).setColumn(45),
                        new ResultMessage("javac", MessageType.ERROR, "package net.ssehub.teaching.submission_check does not exist")
                                .setFile(Path.of("Main.java")).setLine(5).setColumn(59)
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
        
        check.addToClasspath(Path.of("abc"));
        assertThat(check.getClasspath(), is(Arrays.asList(Path.of("abc"))));
        
        check.addToClasspath(Path.of("def"));
        assertThat(check.getClasspath(), is(Arrays.asList(Path.of("abc"), Path.of("def"))));
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
        if (testDirectory != null && Files.isDirectory(testDirectory)) {
            removeClassFiles(testDirectory);
        }
    }
    
    private void removeClassFiles(Path directory) {
        try {
            Files.walk(directory)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".class"))
                .forEach(t -> {
                    try {
                        Files.delete(t);
                    } catch (IOException e) {
                        fail("Cleanup: Failed to delete class file " + t);
                    }
                });
        } catch (IOException e) {
            fail("Cleanup: Failed to delete class files in " + directory);
        }
        
    }
    
}
