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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;

import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.server.checks.eclipse_config.ClasspathEntry.Kind;

public class ClasspathEntryTest {

    @Test
    public void testEquals() {
        ClasspathEntry c1 = new ClasspathEntry(Kind.SOURCE, new File("file1"));
        ClasspathEntry c2 = new ClasspathEntry(Kind.SOURCE, new File("file1"));
        ClasspathEntry c3 = new ClasspathEntry(Kind.LIBRARY, new File("file1"));
        ClasspathEntry c4 = new ClasspathEntry(Kind.SOURCE, new File("file2"));
        
        assertThat(c1.equals(c1), is(true));
        assertThat(c1.equals(c2), is(true));
        
        assertThat(c1.equals(c3), is(false));
        assertThat(c1.equals(c4), is(false));
        
        assertThat(c1.equals(new Object()), is(false));
    }
    
    @Test
    public void testHashCode() {
        ClasspathEntry c1 = new ClasspathEntry(Kind.SOURCE, new File("file1"));
        ClasspathEntry c2 = new ClasspathEntry(Kind.SOURCE, new File("file1"));
        ClasspathEntry c3 = new ClasspathEntry(Kind.LIBRARY, new File("file1"));
        ClasspathEntry c4 = new ClasspathEntry(Kind.SOURCE, new File("file2"));
        
        assertThat(c1.hashCode(), is(c1.hashCode()));
        assertThat(c2.hashCode(), is(c1.hashCode()));
        
        assertThat(c3.hashCode(), is(not(c1.hashCode())));
        assertThat(c4.hashCode(), is(not(c1.hashCode())));
    }
    
    @Test
    public void attributes() {
        ClasspathEntry c1 = new ClasspathEntry(Kind.SOURCE, new File("file1"));
        ClasspathEntry c2 = new ClasspathEntry(Kind.LIBRARY, new File("file1"));
        ClasspathEntry c3 = new ClasspathEntry(Kind.SOURCE, new File("file2"));
        
        assertThat(c1.getKind(), is(Kind.SOURCE));
        assertThat(c2.getKind(), is(Kind.LIBRARY));
        assertThat(c3.getKind(), is(Kind.SOURCE));
        
        assertThat(c1.getPath(), is(new File("file1")));
        assertThat(c2.getPath(), is(new File("file1")));
        assertThat(c3.getPath(), is(new File("file2")));
    }
    
    @Test
    public void testToString() {
        ClasspathEntry c1 = new ClasspathEntry(Kind.SOURCE, new File("file1"));
        ClasspathEntry c2 = new ClasspathEntry(Kind.LIBRARY, new File("file1"));
        ClasspathEntry c3 = new ClasspathEntry(Kind.SOURCE, new File("file2"));
        
        assertThat(c1.toString(), is("ClasspathEntry [kind=SOURCE, path=file1]"));
        assertThat(c2.toString(), is("ClasspathEntry [kind=LIBRARY, path=file1]"));
        assertThat(c3.toString(), is("ClasspathEntry [kind=SOURCE, path=file2]"));
    }
    
}
