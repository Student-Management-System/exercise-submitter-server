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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.server.checks.ResultMessage.MessageType;

public class ResultMessageTest {

    @Test
    public void onlyBasic() {
        ResultMessage message = new ResultMessage("some-tool", MessageType.ERROR, "some message");
        
        assertThat(message.getCheckName(), is("some-tool"));
        assertThat(message.getType(), is(MessageType.ERROR));
        assertThat(message.getMessage(), is("some message"));
        assertThat(message.getFile(), is(nullValue()));
        assertThat(message.getLine(), is(nullValue()));
        assertThat(message.getColumn(), is(nullValue()));
        
        assertThat(message.toString(), is("some-tool error \"some message\""));
    }
    
    @Test
    public void onlyBasicAndFile() {
        ResultMessage message = new ResultMessage("some-other-tool", MessageType.ERROR, "a file message");
        message.setFile(new File("some/file.txt"));
        
        assertThat(message.getCheckName(), is("some-other-tool"));
        assertThat(message.getType(), is(MessageType.ERROR));
        assertThat(message.getMessage(), is("a file message"));
        assertThat(message.getFile(), is(new File("some/file.txt")));
        assertThat(message.getLine(), is(nullValue()));
        assertThat(message.getColumn(), is(nullValue()));
        
        assertThat(message.toString(), is("some-other-tool error in " +  new File("some/file.txt") + " \"a file message\""));
    }
    
    @Test
    public void basicAndFileAndLine() {
        ResultMessage message = new ResultMessage("javac", MessageType.WARNING, "a file message");
        message.setFile(new File("some/file.txt"));
        message.setLine(125);
        
        assertThat(message.getCheckName(), is("javac"));
        assertThat(message.getType(), is(MessageType.WARNING));
        assertThat(message.getMessage(), is("a file message"));
        assertThat(message.getFile(), is(new File("some/file.txt")));
        assertThat(message.getLine(), is(125));
        assertThat(message.getColumn(), is(nullValue()));

        assertThat(message.toString(), is("javac warning in " +  new File("some/file.txt") + ":125 \"a file message\""));
    }
    
    @Test
    public void allFields() {
        ResultMessage message = new ResultMessage("checkstyle", MessageType.WARNING, "now with column");
        message.setFile(new File("Source.java"));
        message.setLine(6);
        message.setColumn(534);
        
        assertThat(message.getType(), is(MessageType.WARNING));
        assertThat(message.getMessage(), is("now with column"));
        assertThat(message.getFile(), is(new File("Source.java")));
        assertThat(message.getLine(), is(6));
        assertThat(message.getColumn(), is(534));

        assertThat(message.toString(), is("checkstyle warning in Source.java:6:534 \"now with column\""));
    }
    
    @Test
    public void equalsMissingFields() {
        ResultMessage full = new ResultMessage("tool", MessageType.WARNING, "some message").setFile(new File("Source.java")).setLine(6).setColumn(534);
        ResultMessage medium = new ResultMessage("tool", MessageType.WARNING, "some message").setFile(new File("Source.java")).setLine(6);
        ResultMessage sparse = new ResultMessage("tool", MessageType.WARNING, "some message");
        
        assertThat(full.equals(full), is(true));
        assertThat(full.equals(medium), is(false));
        assertThat(full.equals(sparse), is(false));
        
        assertThat(medium.equals(full), is(false));
        assertThat(medium.equals(medium), is(true));
        assertThat(medium.equals(sparse), is(false));
        
        assertThat(sparse.equals(full), is(false));
        assertThat(sparse.equals(medium), is(false));
        assertThat(sparse.equals(sparse), is(true));
    }
    
    @Test
    public void equalsDifferentField() {
        ResultMessage v1 = new ResultMessage("toolA", MessageType.WARNING, "some message").setFile(new File("Source.java")).setLine(6).setColumn(534);
        ResultMessage v2 = new ResultMessage("toolB", MessageType.WARNING, "some message").setFile(new File("Source.java")).setLine(6).setColumn(534);
        ResultMessage v3 = new ResultMessage("toolA", MessageType.ERROR, "some message").setFile(new File("Source.java")).setLine(6).setColumn(534);
        ResultMessage v4 = new ResultMessage("toolA", MessageType.WARNING, "other message").setFile(new File("Source.java")).setLine(6).setColumn(534);
        ResultMessage v5 = new ResultMessage("toolA", MessageType.WARNING, "some message").setFile(new File("Other.java")).setLine(6).setColumn(534);
        ResultMessage v6 = new ResultMessage("toolA", MessageType.WARNING, "some message").setFile(new File("Source.java")).setLine(654).setColumn(534);
        ResultMessage v7 = new ResultMessage("toolA", MessageType.WARNING, "some message").setFile(new File("Source.java")).setLine(6).setColumn(5);
        
        ResultMessage[] array = {v1, v2, v3, v4, v5, v6, v7};
        
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array.length; j++) {
                assertThat(array[i].equals(array[j]), is(i == j));
                assertThat(array[i].hashCode() == array[j].hashCode(), is(i == j));
            }
        }
    }
    
    @Test
    public void equalsDifferentInstance() {
        ResultMessage one = new ResultMessage("javac", MessageType.WARNING, "some message").setFile(new File("Source.java")).setLine(6).setColumn(534);
        ResultMessage two = new ResultMessage("javac", MessageType.WARNING, "some message").setFile(new File("Source.java")).setLine(6).setColumn(534);
        
        assertThat(one.equals(two), is(true));
        assertThat(two.equals(one), is(true));
    }
    
    @Test
    public void equalsDifferentClass() {
        assertThat(new ResultMessage("checkstyle", MessageType.ERROR, "abc").equals(new Object()), is(false));
    }
    
    @Test
    public void sortingByLocation() {
        ResultMessage m0 = new ResultMessage("checkstyle", MessageType.ERROR, "other tool").setFile(new File("A.txt"));
        ResultMessage m1 = new ResultMessage("javac", MessageType.ERROR, "no file");
        ResultMessage m2 = new ResultMessage("javac", MessageType.ERROR, "first file").setFile(new File("A.txt"));
        ResultMessage m3 = new ResultMessage("javac", MessageType.ERROR, "second file").setFile(new File("B.txt"));
        ResultMessage m4 = new ResultMessage("javac", MessageType.ERROR, "second file with line number").setFile(new File("B.txt")).setLine(17);
        ResultMessage m5 = new ResultMessage("javac", MessageType.ERROR, "second file with line number").setFile(new File("B.txt")).setLine(19);
        ResultMessage m6 = new ResultMessage("javac", MessageType.ERROR, "second file with line number and column").setFile(new File("B.txt")).setLine(19).setColumn(2);
        ResultMessage m7 = new ResultMessage("javac", MessageType.ERROR, "second file with line number and column").setFile(new File("B.txt")).setLine(19).setColumn(5);
        
        List<ResultMessage> messages = new LinkedList<>();
        messages.add(m7);
        messages.add(m6);
        messages.add(m5);
        messages.add(m4);
        messages.add(m3);
        messages.add(m2);
        messages.add(m1);
        messages.add(m0);
        
        Collections.sort(messages);
        
        assertThat(messages, is(Arrays.asList(m0, m1, m2, m3, m4, m5, m6, m7)));
    }
    
    @Test
    public void compareWithEmptyFields() {
        ResultMessage bare = new ResultMessage("javac", MessageType.ERROR, "msg");
        ResultMessage withFile = new ResultMessage("javac", MessageType.ERROR, "msg").setFile(new File("some"));
        ResultMessage withLine = new ResultMessage("javac", MessageType.ERROR, "msg").setFile(new File("some")).setLine(1);
        ResultMessage withColumn = new ResultMessage("javac", MessageType.ERROR, "msg").setFile(new File("some")).setLine(1).setColumn(1);
        
        ResultMessage[] arraySorted = {bare, withFile, withLine, withColumn};
        
        for (int i = 0; i < arraySorted.length; i++) {
            for (int j = 0; j < arraySorted.length; j++) {
                int expected;
                if (i < j) {
                    expected = -1;
                } else if (i == j) {
                    expected = 0;
                } else {
                    expected = 1;
                }
                
                assertThat(arraySorted[i] + " compared to " + arraySorted[j],
                        arraySorted[i].compareTo(arraySorted[j]), is(expected));
            }
        }
    }
    
    @Test
    public void compareWithinFields() {
        ResultMessage check1 = new ResultMessage("a", MessageType.WARNING, "msg");
        ResultMessage check2 = new ResultMessage("b", MessageType.WARNING, "msg");
        assertThat(check1.compareTo(check2), is(-1));
        assertThat(check2.compareTo(check1), is(1));
        
        ResultMessage file1 = new ResultMessage("javac", MessageType.WARNING, "msg").setFile(new File("A"));
        ResultMessage file2 = new ResultMessage("javac", MessageType.WARNING, "msg").setFile(new File("B"));
        assertThat(file1.compareTo(file2), is(-1));
        assertThat(file2.compareTo(file1), is(1));
        
        ResultMessage line1 = new ResultMessage("javac", MessageType.WARNING, "msg").setFile(new File("A")).setLine(15);
        ResultMessage line2 = new ResultMessage("javac", MessageType.WARNING, "msg").setFile(new File("A")).setLine(17);
        assertThat(line1.compareTo(line2), is(-1));
        assertThat(line2.compareTo(line1), is(1));
        
        ResultMessage column1 = new ResultMessage("javac", MessageType.WARNING, "msg").setFile(new File("A")).setLine(15).setColumn(3);
        ResultMessage column2 = new ResultMessage("javac", MessageType.WARNING, "msg").setFile(new File("A")).setLine(15).setColumn(5);
        assertThat(column1.compareTo(column2), is(-1));
        assertThat(column2.compareTo(column1), is(1));
    }
    
}
