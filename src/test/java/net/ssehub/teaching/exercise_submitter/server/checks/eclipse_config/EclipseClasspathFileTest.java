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
package net.ssehub.teaching.exercise_submitter.server.checks.eclipse_config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.server.checks.eclipse_config.ClasspathEntry.Kind;

public class EclipseClasspathFileTest {
    
    private static final File TESTDATA = new File("src/test/resources/EclipseClasspathFileTest");

    @Test
    public void notExistingFile() throws InvalidEclipseConfigException, IOException {
        File file = new File(TESTDATA, "doesnt_exist");
        assertThat("Precondition: test file should not exist",
                file.exists(), is(false));
        
        assertThrows(FileNotFoundException.class, () -> {
            new EclipseClasspathFile(file);
        });
    }
    
    @Test
    public void invalidXml() throws InvalidEclipseConfigException, IOException {
        File file = new File(TESTDATA, "invalidXml.xml");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        InvalidEclipseConfigException exc = assertThrows(InvalidEclipseConfigException.class, () -> {
            new EclipseClasspathFile(file);
        });
        
        assertThat("Postcondition: exception has correct message",
                exc.getMessage(), is("Could not parse XML"));
    }
    
    @Test
    public void invalidRoot() throws InvalidEclipseConfigException, IOException {
        File file = new File(TESTDATA, "invalidRoot.xml");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        InvalidEclipseConfigException exc = assertThrows(InvalidEclipseConfigException.class, () -> {
            new EclipseClasspathFile(file);
        });
        
        assertThat("Postcondition: exception has correct message",
                exc.getMessage(), is("Invalid root node: not_classpath"));
    }
    
    @Test
    public void noEntries()  throws InvalidEclipseConfigException, IOException {
        File file = new File(TESTDATA, "noEntries.xml");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        EclipseClasspathFile result = new EclipseClasspathFile(file);
        
        assertThat("Postcondition: should contain no classpath entries",
                result.getClasspathEntries(), is(Arrays.asList()));
    }
    
    @Test
    public void invalidChild() throws InvalidEclipseConfigException, IOException {
        File file = new File(TESTDATA, "invalidChild.xml");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        InvalidEclipseConfigException exc = assertThrows(InvalidEclipseConfigException.class, () -> {
            new EclipseClasspathFile(file);
        });

        assertThat("Postcondition: exception has correct message",
                exc.getMessage(), is("Invalid node in <classpath>: somethingelse"));
    }
    
    @Test
    public void singleEntry()  throws InvalidEclipseConfigException, IOException {
        File file = new File(TESTDATA, "singleEntry.xml");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        EclipseClasspathFile result = new EclipseClasspathFile(file);
        
        assertThat("Postcondition: should contain correct classpath entries",
                result.getClasspathEntries(), is(Arrays.asList(
                        new ClasspathEntry(Kind.SOURCE, new File("src"))
                )));
    }
    
    @Test
    public void multipleEntries()  throws InvalidEclipseConfigException, IOException {
        File file = new File(TESTDATA, "multipleEntries.xml");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        EclipseClasspathFile result = new EclipseClasspathFile(file);
        
        assertThat("Postcondition: should contain correct classpath entries",
                result.getClasspathEntries(), is(Arrays.asList(
                        new ClasspathEntry(Kind.SOURCE, new File("src")),
                        new ClasspathEntry(Kind.CONTAINER, new File("org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-11")),
                        new ClasspathEntry(Kind.LIBRARY, new File("libs/commons-io-2.4.jar")),
                        new ClasspathEntry(Kind.OUTPUT, new File("bin"))
                )));
    }
    
    @Test
    public void missingKind()  throws InvalidEclipseConfigException, IOException {
        File file = new File(TESTDATA, "missingKind.xml");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        InvalidEclipseConfigException exc = assertThrows(InvalidEclipseConfigException.class, () -> {
            new EclipseClasspathFile(file);
        });

        assertThat("Postcondition: exception has correct message",
                exc.getMessage(), is("Entry does not have a \"kind\" attribute"));
    }
    
    @Test
    public void missingPath()  throws InvalidEclipseConfigException, IOException {
        File file = new File(TESTDATA, "missingPath.xml");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        InvalidEclipseConfigException exc = assertThrows(InvalidEclipseConfigException.class, () -> {
            new EclipseClasspathFile(file);
        });

        assertThat("Postcondition: exception has correct message",
                exc.getMessage(), is("Entry does not have a \"path\" attribute"));
    }
    
    @Test
    public void otherKind()  throws InvalidEclipseConfigException, IOException {
        File file = new File(TESTDATA, "otherKind.xml");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        EclipseClasspathFile result = new EclipseClasspathFile(file);
        
        assertThat("Postcondition: should contain correct classpath entries",
                result.getClasspathEntries(), is(Arrays.asList(
                        new ClasspathEntry(Kind.OTHER, new File("src"))
                )));
    }
    
    @Test
    public void furtherAttributes()  throws InvalidEclipseConfigException, IOException {
        File file = new File(TESTDATA, "furtherAttributes.xml");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        EclipseClasspathFile result = new EclipseClasspathFile(file);
        
        assertThat("Postcondition: should contain correct classpath entries",
                result.getClasspathEntries(), is(Arrays.asList(
                        new ClasspathEntry(Kind.SOURCE, new File("src"))
                )));
    }
    
    @Test
    public void nestedAttributes()  throws InvalidEclipseConfigException, IOException {
        File file = new File(TESTDATA, "nestedAttributes.xml");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        EclipseClasspathFile result = new EclipseClasspathFile(file);
        
        assertThat("Postcondition: should contain correct classpath entries",
                result.getClasspathEntries(), is(Arrays.asList(
                        new ClasspathEntry(Kind.CONTAINER, new File("org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-11"))
                )));
    }
    
    @Test
    public void listIsUnmodifiable()  throws InvalidEclipseConfigException, IOException {
        File file = new File(TESTDATA, "noEntries.xml");
        assertThat("Precondition: test file should exist",
                file.isFile(), is(true));
        
        EclipseClasspathFile result = new EclipseClasspathFile(file);
        
        assertThrows(UnsupportedOperationException.class, () -> {
            result.getClasspathEntries().add(new ClasspathEntry(Kind.SOURCE, new File("abc")));
        });
    }
    
}
