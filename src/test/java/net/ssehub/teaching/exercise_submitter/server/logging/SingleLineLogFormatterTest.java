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
package net.ssehub.teaching.exercise_submitter.server.logging;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SingleLineLogFormatterTest {

    private Logger testLogger;
    
    private ByteArrayOutputStream logBuffer;
    
    private StreamHandler streamHandler;
    
    @BeforeEach
    public void setupTestLogger() throws SecurityException, UnsupportedEncodingException {
        testLogger = Logger.getLogger(SingleLineLogFormatterTest.class.getName());
        testLogger.setUseParentHandlers(false);
        
        logBuffer = new ByteArrayOutputStream();
        
        streamHandler = new StreamHandler(logBuffer, new SingleLineLogFormatter());
        streamHandler.setEncoding("UTF-8");
        streamHandler.setLevel(Level.ALL);
        testLogger.setLevel(Level.ALL);
        testLogger.addHandler(streamHandler);
    }
    
    @AfterEach
    public void closeStreamHandler() {
        streamHandler.close();
    }
    
    @Test
    public void simpleLine() {
        testLogger.log(Level.INFO, "Some simple message");
        
        List<String> lines = getLogLines();
        
        assertLine(lines.get(0), " [INFO] [SingleLineLogFormatterTest.simpleLine] Some simple message");
        assertThat(lines.size(), is(1));
    }
    
    @Test
    public void multipleSimpleLines() {
        testLogger.log(Level.INFO, "Some simple message");
        testLogger.log(Level.INFO, "And another message");
        testLogger.log(Level.INFO, "A third one, too");
        
        List<String> lines = getLogLines();
        
        assertLine(lines.get(0), " [INFO] [SingleLineLogFormatterTest.multipleSimpleLines] Some simple message");
        assertLine(lines.get(1), " [INFO] [SingleLineLogFormatterTest.multipleSimpleLines] And another message");
        assertLine(lines.get(2), " [INFO] [SingleLineLogFormatterTest.multipleSimpleLines] A third one, too");
        assertThat(lines.size(), is(3));
    }
    
    @Test
    public void umlauts() {
        testLogger.log(Level.INFO, "We häve söme Ümläuts");
        
        List<String> lines = getLogLines();
        
        assertLine(lines.get(0), " [INFO] [SingleLineLogFormatterTest.umlauts] We häve söme Ümläuts");
        assertThat(lines.size(), is(1));
    }
    
    @Test
    public void levels() {
        testLogger.log(Level.INFO, "Msg1");
        testLogger.log(Level.WARNING, "Msg2");
        testLogger.log(Level.SEVERE, "Msg3");
        testLogger.log(Level.CONFIG, "Msg4");
        testLogger.log(Level.FINE, "Msg5");
        testLogger.log(Level.FINER, "Msg6");
        testLogger.log(Level.FINEST, "Msg7");
        testLogger.log(Level.ALL, "Msg8");
        
        List<String> lines = getLogLines();
        
        assertLine(lines.get(0), " [INFO] [SingleLineLogFormatterTest.levels] Msg1");
        assertLine(lines.get(1), " [WARNING] [SingleLineLogFormatterTest.levels] Msg2");
        assertLine(lines.get(2), " [SEVERE] [SingleLineLogFormatterTest.levels] Msg3");
        assertLine(lines.get(3), " [CONFIG] [SingleLineLogFormatterTest.levels] Msg4");
        assertLine(lines.get(4), " [FINE] [SingleLineLogFormatterTest.levels] Msg5");
        assertLine(lines.get(5), " [FINER] [SingleLineLogFormatterTest.levels] Msg6");
        assertLine(lines.get(6), " [FINEST] [SingleLineLogFormatterTest.levels] Msg7");
        assertLine(lines.get(7), " [ALL] [SingleLineLogFormatterTest.levels] Msg8");
        assertThat(lines.size(), is(8));
    }
    
    static class SomeInnerClass {
        
        public static void someMethod(Logger logger) {
            logger.log(Level.INFO, "logmessage");
        }
        
    }
    
    @Test
    public void otherSource() {
        SomeInnerClass.someMethod(testLogger);
        
        List<String> lines = getLogLines();
        
        assertLine(lines.get(0), " [INFO] [SingleLineLogFormatterTest$SomeInnerClass.someMethod] logmessage");
        assertThat(lines.size(), is(1));
    }
    
    @Test
    public void noSourceButLoggerName() {
        LogRecord record = new LogRecord(Level.INFO, "logmessage");
        record.setSourceClassName(null);
        record.setLoggerName(testLogger.getName());
        
        testLogger.log(record);
        
        List<String> lines = getLogLines();
        
        assertLine(lines.get(0), " [INFO] [SingleLineLogFormatterTest] logmessage");
        assertThat(lines.size(), is(1));
    }
    
    @Test
    public void noSourceNoLoggerName() {
        LogRecord record = new LogRecord(Level.INFO, "logmessage");
        record.setSourceClassName(null);
        record.setLoggerName(null);
        
        testLogger.log(record);
        
        List<String> lines = getLogLines();
        
        assertLine(lines.get(0), " [INFO] logmessage");
        assertThat(lines.size(), is(1));
    }
    
    @Test
    public void noMethodName() {
        LogRecord record = new LogRecord(Level.INFO, "logmessage");
        record.setSourceClassName(SingleLineLogFormatterTest.class.getName());
        record.setSourceMethodName(null);
        
        testLogger.log(record);
        
        List<String> lines = getLogLines();
        
        assertLine(lines.get(0), " [INFO] [SingleLineLogFormatterTest] logmessage");
        assertThat(lines.size(), is(1));
    }
    
    @Test
    public void noDotInClassName() {
        LogRecord record = new LogRecord(Level.INFO, "logmessage");
        record.setSourceClassName("NotInPackage");
        record.setSourceMethodName("noDotInClassName");
        
        testLogger.log(record);
        
        List<String> lines = getLogLines();
        
        assertLine(lines.get(0), " [INFO] [NotInPackage.noDotInClassName] logmessage");
        assertThat(lines.size(), is(1));
    }
    
    @Test
    public void messageFormat() {
        testLogger.log(Level.INFO, "a: {0}, b: {1}", new Object[] {1, Path.of("test.txt")});
        
        List<String> lines = getLogLines();
        
        assertLine(lines.get(0), " [INFO] [SingleLineLogFormatterTest.messageFormat] a: 1, b: test.txt");
        assertThat(lines.size(), is(1));
    }
    
    @Test
    public void exception() {
        IOException exception = new IOException("exception message");
        StackTraceElement[] stackTrace = {
                new StackTraceElement("some.class.Name", "someMethod", "Name.java", 124),
                new StackTraceElement("some.other.ClassName", "main", "Main.java", 52),
        };
        exception.setStackTrace(stackTrace);
        
        testLogger.log(Level.WARNING, "An exception", exception);
        
        List<String> lines = getLogLines();
        
        assertLine(lines.get(0), " [WARNING] [SingleLineLogFormatterTest.exception] An exception");
        assertThat(lines.get(1), is(" ".repeat(21 + 50) + "java.io.IOException: exception message"));
        assertThat(lines.get(2), is(" ".repeat(21 + 50) + "  at some.class.Name.someMethod(Name.java:124)"));
        assertThat(lines.get(3), is(" ".repeat(21 + 50) + "  at some.other.ClassName.main(Main.java:52)"));
        
        assertThat(lines.size(), is(4));
    }
    
    @Test
    public void exceptionNoMessage() {
        IOException exception = new IOException();
        StackTraceElement[] stackTrace = {
                new StackTraceElement("some.class.Name", "someMethod", "Name.java", 124),
                new StackTraceElement("some.other.ClassName", "main", "Main.java", 52),
        };
        exception.setStackTrace(stackTrace);
        
        testLogger.log(Level.WARNING, "", exception);
        
        List<String> lines = getLogLines();
        
        assertLine(lines.get(0), " [WARNING] [SingleLineLogFormatterTest.exceptionNoMessage] ");
        assertThat(lines.get(1), is(" ".repeat(21 + 59) + "java.io.IOException"));
        assertThat(lines.get(2), is(" ".repeat(21 + 59) + "  at some.class.Name.someMethod(Name.java:124)"));
        assertThat(lines.get(3), is(" ".repeat(21 + 59) + "  at some.other.ClassName.main(Main.java:52)"));
        
        assertThat(lines.size(), is(4));
    }
    
    @Test
    public void exceptionWithCause() {
        IOException cause = new IOException();
        StackTraceElement[] causeTrace = {
                new StackTraceElement("some.class.Name", "someMethod", "Name.java", 124),
                new StackTraceElement("some.other.ClassName", "main", "Main.java", 52),
        };
        cause.setStackTrace(causeTrace);
        
        UncheckedIOException exception = new UncheckedIOException("got IO error", cause);
        StackTraceElement[] excTrace = {
                new StackTraceElement("random.Class", "throwError", "Random.java", 52),
                new StackTraceElement("random.Class", "process", "Random.java", 24),
        };
        exception.setStackTrace(excTrace);
        
        testLogger.log(Level.WARNING, "An exception", exception);
        
        List<String> lines = getLogLines();
        
        assertLine(lines.get(0), " [WARNING] [SingleLineLogFormatterTest.exceptionWithCause] An exception");
        assertThat(lines.get(1), is(" ".repeat(21 + 59) + "java.io.UncheckedIOException: got IO error"));
        assertThat(lines.get(2), is(" ".repeat(21 + 59) + "  at random.Class.throwError(Random.java:52)"));
        assertThat(lines.get(3), is(" ".repeat(21 + 59) + "  at random.Class.process(Random.java:24)"));
        assertThat(lines.get(4), is(" ".repeat(21 + 59) + "Caused by:"));
        assertThat(lines.get(5), is(" ".repeat(21 + 59) + "java.io.IOException"));
        assertThat(lines.get(6), is(" ".repeat(21 + 59) + "  at some.class.Name.someMethod(Name.java:124)"));
        assertThat(lines.get(7), is(" ".repeat(21 + 59) + "  at some.other.ClassName.main(Main.java:52)"));
        
        assertThat(lines.size(), is(8));
    }
    
    @Test
    public void exceptionFollowedByMessage() {
        IOException exception = new IOException("exception message");
        StackTraceElement[] stackTrace = {
                new StackTraceElement("some.class.Name", "someMethod", "Name.java", 124),
                new StackTraceElement("some.other.ClassName", "main", "Main.java", 52),
        };
        exception.setStackTrace(stackTrace);
        
        testLogger.log(Level.WARNING, "An exception", exception);
        testLogger.log(Level.INFO, "Some other message");
        
        List<String> lines = getLogLines();
        
        assertLine(lines.get(0), " [WARNING] [SingleLineLogFormatterTest.exceptionFollowedByMessage] An exception");
        assertThat(lines.get(1), is(" ".repeat(21 + 67) + "java.io.IOException: exception message"));
        assertThat(lines.get(2), is(" ".repeat(21 + 67) + "  at some.class.Name.someMethod(Name.java:124)"));
        assertThat(lines.get(3), is(" ".repeat(21 + 67) + "  at some.other.ClassName.main(Main.java:52)"));
        assertLine(lines.get(4), " [INFO] [SingleLineLogFormatterTest.exceptionFollowedByMessage] Some other message");
        
        assertThat(lines.size(), is(5));
    }
    
    private void assertLine(String actual, String expectedContentWithoutDate) {
        assertThat("Start of string should match date format (got: " + actual.substring(0, 20) + ")",
                actual.substring(0, 21).matches("\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}\\]"), is(true));
        
        assertThat(actual.substring(21), is(expectedContentWithoutDate));
    }
    
    private List<String> getLogLines() {
        streamHandler.flush();
        
        return Arrays.asList(logBuffer.toString(StandardCharsets.UTF_8).split("\n"));
    }
    
    
    
}
