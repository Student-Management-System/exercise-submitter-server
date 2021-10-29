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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

public class CheckstyleCheckIT {

    private static final Path TESTDATA = Path.of("src/test/resources/CheckstyleCheckTest");
    
    private static final Path BEGINNERS_RULES = TESTDATA.resolve("javaBeginners_checks.xml");
    
    private static final Path OO_RULES = TESTDATA.resolve("javaOO_checks.xml");
    
    private static final Path JAVADOC_RULES = TESTDATA.resolve("javaWithDocs_checks.xml");
    
    private static final Path UMLAUTS_ALLOWED = TESTDATA.resolve("umlauts_allowed.xml");
    
    private static final Path MODIFIER_ORDER_WARNING = TESTDATA.resolve("modifier_order_warning.xml");
    
    private static final Path MODIFIER_ORDER_INFO = TESTDATA.resolve("modifier_order_info.xml");
    
    private static final Path MODIFIER_ORDER_IGNORE = TESTDATA.resolve("modifier_order_ignore.xml");
    
    private static final Path INVALID_RULES = TESTDATA.resolve("invalid_rules.xml");
    
    @Test
    @DisplayName("succeeds on submission with no Java files")
    public void noJavaFiles() {
        Path directory = TESTDATA.resolve("noJavaFiles");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(directory));
        
        CheckstyleCheck check = new CheckstyleCheck(BEGINNERS_RULES);
        
        assertThat("Precondition: should not have any result messages before the execution",
                check.getResultMessages(), is(Arrays.asList()));
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should not create any result messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("correctly formatted file should succeed")
    public void beginnersCorrect() {
        Path directory = TESTDATA.resolve("beginnersCorrect");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(directory));
        
        CheckstyleCheck check = new CheckstyleCheck(BEGINNERS_RULES);
        
        assertThat("Precondition: should not have any result messages before the execution",
                check.getResultMessages(), is(Arrays.asList()));

        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should not create any result messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("incorrectly formatted file should not succeed")
    public void beginnersIncorrect() {
        Path directory = TESTDATA.resolve("beginnersIncorrect");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(directory));
        
        CheckstyleCheck check = new CheckstyleCheck(BEGINNERS_RULES);
        
        assertThat("Precondition: should not have any result messages before the execution",
                check.getResultMessages(), is(Arrays.asList()));
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create result messages", check.getResultMessages(), containsInAnyOrder(
                    new ResultMessage("checkstyle", MessageType.ERROR,
                            "'method def' child has incorrect indentation level 6, expected level should be 8")
                            .setFile(Path.of("HelloWorld.java")).setLine(4).setColumn(7),
                    new ResultMessage("checkstyle", MessageType.ERROR, "';' is preceded with whitespace")
                            .setFile(Path.of("HelloWorld.java")).setLine(4).setColumn(42)
                ))
        );
    }
    
    @Test
    @DisplayName("creates correct message for non-parseable Java file")
    public void notCompiling() {
        Path directory = TESTDATA.resolve("notCompiling");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(directory));
        
        CheckstyleCheck check = new CheckstyleCheck(BEGINNERS_RULES);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create a result message", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("checkstyle", MessageType.ERROR, "Checkstyle could not parse file").setFile(Path.of("HelloWorld.java"))
                )))
        );
    }
    
    @Test
    @DisplayName("creates internal error message for non-existant rules file")
    public void notExistingCheckstyleRules() {
        Path directory = TESTDATA.resolve("beginnersCorrect");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(directory));
        
        Path rulesFile = Path.of("doesnt_exist");
        assertThat("Precondition: rules file should not exist", !Files.exists(rulesFile));
        
        CheckstyleCheck check = new CheckstyleCheck(rulesFile);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create a result message", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("checkstyle", MessageType.ERROR, "An internal error occurred while running Checkstyle")
                )))
        );
    }
    
    @Test
    @DisplayName("creates internal error message for invalid rules file")
    public void invalidCheckstyleRules() {
        Path directory = TESTDATA.resolve("beginnersCorrect");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(directory));
        
        CheckstyleCheck check = new CheckstyleCheck(INVALID_RULES);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create a result message", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("checkstyle", MessageType.ERROR, "An internal error occurred while running Checkstyle")
                )))
        );
    }
    
    @Test
    @DisplayName("does not succeed with Javadoc rules on beginners-formatted file")
    public void javadocOnBeginners() {
        Path directory = TESTDATA.resolve("beginnersCorrect");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(directory));
        
        CheckstyleCheck check = new CheckstyleCheck(JAVADOC_RULES);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create result messages", check.getResultMessages(), containsInAnyOrder(
                    new ResultMessage("checkstyle", MessageType.ERROR, "Missing a Javadoc comment")
                        .setFile(Path.of("HelloWorld.java")).setLine(1).setColumn(1),
                    new ResultMessage("checkstyle", MessageType.ERROR, "Missing a Javadoc comment")
                        .setFile(Path.of("HelloWorld.java")).setLine(3).setColumn(5)
                ))
        );
    }
    
    @Test
    @DisplayName("succeeds with OO rules on correctly formatted files in packages")
    public void ooOnPackages() {
        Path directory = TESTDATA.resolve("packagesNoJavadoc");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(directory));
        
        CheckstyleCheck check = new CheckstyleCheck(OO_RULES);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should not create any result messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("does not succeed with Javadoc rules on files in packages without Javadoc comments")
    public void javadocOnPackages() {
        Path directory = TESTDATA.resolve("packagesNoJavadoc");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(directory));
        
        CheckstyleCheck check = new CheckstyleCheck(JAVADOC_RULES);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create result messages", check.getResultMessages(), containsInAnyOrder(
                    new ResultMessage("checkstyle", MessageType.ERROR, "Missing a Javadoc comment")
                        .setFile(Path.of("main/Main.java")).setLine(5).setColumn(1),
                    new ResultMessage("checkstyle", MessageType.ERROR, "Missing a Javadoc comment")
                        .setFile(Path.of("main/Main.java")).setLine(7).setColumn(5),
                    new ResultMessage("checkstyle", MessageType.ERROR, "Missing a Javadoc comment")
                        .setFile(Path.of("util/Util.java")).setLine(3).setColumn(1),
                    new ResultMessage("checkstyle", MessageType.ERROR, "Missing a Javadoc comment")
                        .setFile(Path.of("util/Util.java")).setLine(5).setColumn(5)
                ))
        );
    }
    
    @Test
    @DisplayName("succeeds on correct UTF-8 encoded umlauts")
    public void utf8UmlautsCorrect() {
        Path directory = TESTDATA.resolve("umlauts");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(directory));
        
        CheckstyleCheck check = new CheckstyleCheck(UMLAUTS_ALLOWED);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should not create any result messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("does not succeed on umlauts in wrong encoding")
    public void iso88591UmlautsIncorrect() {
        Path directory = TESTDATA.resolve("umlauts_ISO-8859-1");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(directory));
        
        CheckstyleCheck check = new CheckstyleCheck(UMLAUTS_ALLOWED);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should not succeed", success, is(false)),
            () -> assertThat("Postcondition: should create a result message", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("checkstyle", MessageType.ERROR, "Name '�' must match pattern '^[a-zöäüß][a-zöäüßA-ZÄÖÜ0-9]*$'").setFile(Path.of("Umlauts.java")).setLine(4).setColumn(13)
                )))
        );
    }
    
    @Test
    @DisplayName("succeeds on correct ISO-8859-1 encoded umlauts")
    public void iso88591UmlautsCorrect() {
        Path directory = TESTDATA.resolve("umlauts_ISO-8859-1");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(directory));
        
        CheckstyleCheck check = new CheckstyleCheck(UMLAUTS_ALLOWED);
        check.setCharset(StandardCharsets.ISO_8859_1);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should not create any result messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("creates correct result message for check with warning severity")
    public void warningSeverity() {
        Path directory = TESTDATA.resolve("modifierOrderIncorrect");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(directory));
        
        CheckstyleCheck check = new CheckstyleCheck(MODIFIER_ORDER_WARNING);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should create a warning result message", check.getResultMessages(), is(Arrays.asList(
                    new ResultMessage("checkstyle", MessageType.WARNING, "'public' modifier out of order with the JLS suggestions")
                        .setFile(Path.of("HelloWorld.java")).setLine(3).setColumn(12)
                )))
        );
    }
    
    @Test
    @DisplayName("creates no result message for check with info severity")
    public void infoSeverity() {
        Path directory = TESTDATA.resolve("modifierOrderIncorrect");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(directory));
        
        CheckstyleCheck check = new CheckstyleCheck(MODIFIER_ORDER_INFO);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should not create any result messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("creates no result message for check with ignore severity")
    public void ignoreSeverity() {
        Path directory = TESTDATA.resolve("modifierOrderIncorrect");
        assertThat("Precondition: directory with test files does not exist",
                Files.isDirectory(directory));
        
        CheckstyleCheck check = new CheckstyleCheck(MODIFIER_ORDER_IGNORE);
        
        boolean success = check.run(directory);
        
        assertAll(
            () -> assertThat("Postcondition: should succeed", success, is(true)),
            () -> assertThat("Postcondition: should not create any result messages", check.getResultMessages(), is(Arrays.asList()))
        );
    }
    
    @Test
    @DisplayName("getters return previously set values")
    public void getters() {
        CheckstyleCheck check = new CheckstyleCheck(Path.of("abc.xml"));
        
        assertThat(check.getCheckstyleRules(), is(Path.of("abc.xml")));
        assertThat("should return correct default value",
                check.getCharset(), is(StandardCharsets.UTF_8));
        
        check.setCharset(StandardCharsets.ISO_8859_1);
        assertThat(check.getCharset(), is(StandardCharsets.ISO_8859_1));
        
        check = new CheckstyleCheck(Path.of("something/else.xml"));
        assertThat(check.getCheckstyleRules(), is(Path.of("something/else.xml")));
    }
    
    @BeforeAll
    public static void checkRulesExist() {
        assertAll(
            () -> assertThat("Precondition: Checkstyle beginners rule file should exist (" + BEGINNERS_RULES + ")",
                    Files.isRegularFile(BEGINNERS_RULES)),
            () ->  assertThat("Precondition: Checkstyle OO rule file should exist (" + OO_RULES + ")",
                    Files.isRegularFile(OO_RULES)),
            () -> assertThat("Precondition: Checkstyle javadoc rule file should exist (" + JAVADOC_RULES + ")",
                    Files.isRegularFile(JAVADOC_RULES)),
            () -> assertThat("Precondition: Checkstyle umlauts rule file should exist (" + UMLAUTS_ALLOWED + ")",
                    Files.isRegularFile(UMLAUTS_ALLOWED)),
            () -> assertThat("Precondition: Checkstyle warning modifier order rule file should exist (" + MODIFIER_ORDER_WARNING + ")",
                    Files.isRegularFile(MODIFIER_ORDER_WARNING)),
            () -> assertThat("Precondition: Checkstyle info modifier order rule file should exist (" + MODIFIER_ORDER_INFO + ")",
                    Files.isRegularFile(MODIFIER_ORDER_INFO)),
            () -> assertThat("Precondition: Checkstyle ignore modifier order rule file should exist (" + MODIFIER_ORDER_IGNORE + ")",
                    Files.isRegularFile(MODIFIER_ORDER_IGNORE)),
            () -> assertThat("Precondition: Checkstyle invalid rule file should exist (" + INVALID_RULES + ")",
                    Files.isRegularFile(INVALID_RULES))
        );
    }
    
}
