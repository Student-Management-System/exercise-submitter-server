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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LoggingSetupTest {

    public static final Logger ROOT_LOGGER = Logger.getLogger("");
    
    private UncaughtExceptionHandler previousUncaughtExceptionHandler;
    
    private Handler[] previousHandlers;
    
    @BeforeEach
    public void removeLoggingHandlers() {
        previousUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(null);
        
        previousHandlers = ROOT_LOGGER.getHandlers();
        Arrays.stream(previousHandlers).forEach(ROOT_LOGGER::removeHandler);
    }
    
    @AfterEach
    public void restoreLoggingHandlers() {
        Thread.setDefaultUncaughtExceptionHandler(previousUncaughtExceptionHandler);

        Arrays.stream(previousHandlers).forEach(ROOT_LOGGER::addHandler);
    }
    
    @Test
    public void initAddsStdoutHandler() throws IOException {
        PrintStream oldOut = System.out;
        
        try {
            assertThat("Precondition: no handlers in root logger",
                    ROOT_LOGGER.getHandlers().length, is(0));
            
            ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
            
            System.setOut(new PrintStream(stdoutBuffer));
            
            LoggingSetup.init();
            
            assertThat("Postcondition: a single handler in root logger",
                    ROOT_LOGGER.getHandlers().length, is(1));
            
            Handler handler = ROOT_LOGGER.getHandlers()[0];
            assertThat("Postcondition: Handler is StreamHandler",
                    handler, is(instanceOf(StreamHandler.class)));
            
            assertThat("Postcondition: custom formatter is set",
                    handler.getFormatter(), instanceOf(SingleLineLogFormatter.class));
            
            assertThat("Postcondition: UTF-8 encoding ist set",
                    handler.getEncoding(), is("UTF-8"));
            
            assertThat("Postcondition: Handler level is set to ALL",
                    handler.getLevel(), is(Level.ALL));
            
            assertThat("Postcondition: Logger level is set to INFO",
                    ROOT_LOGGER.getLevel(), is(Level.INFO));
            
            ROOT_LOGGER.log(Level.INFO, "some test log message");
            
            handler.flush();
            
            assertThat("Postcondition: log messages should appear in stdout",
                    stdoutBuffer.toString().contains("some test log message"), is(true));
            
        } finally {
            System.setOut(oldOut);
        }
    }
    
    @Test
    public void initSetsUpUncaughtExceptionHandler() throws IOException, InterruptedException {
        PrintStream oldOut = System.out;
        
        try {
            assertThat("Precondition: no uncaught exception handler",
                    Thread.getDefaultUncaughtExceptionHandler(), is(nullValue()));
            
            ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
            
            System.setOut(new PrintStream(stdoutBuffer));
            
            LoggingSetup.init();
            
            Handler handler = ROOT_LOGGER.getHandlers()[0];
            
            assertThat("Postcondition: uncaught exception handler registered",
                    Thread.getDefaultUncaughtExceptionHandler(), not(nullValue()));
            
            Thread crashingThread = new Thread(() -> {
                throw new RuntimeException("test error message");
            }, "CrashingTestThread");
            crashingThread.start();
            
            crashingThread.join();
            
            handler.flush();
            
            assertThat("Postcondition: log messages should appear in stdout",
                    stdoutBuffer.toString().contains("Uncaught exception in thread CrashingTestThread"), is(true));
            assertThat("Postcondition: log messages should appear in stdout",
                    stdoutBuffer.toString().contains("java.lang.RuntimeException: test error message"), is(true));
            
            Thread.setDefaultUncaughtExceptionHandler(null);
            assertThat("Cleanup: no uncaught exception handler",
                    Thread.getDefaultUncaughtExceptionHandler(), is(nullValue()));
            
        } finally {
            System.setOut(oldOut);
        }
    }
    
    @Test
    public void initRemovesPreviousHandlers() throws IOException {
        assertThat("Precondition: no handlers in root logger",
                ROOT_LOGGER.getHandlers().length, is(0));
        
        Path tempFile = Files.createTempFile("exercise-submitter-server-logging-test", ".log");
        
        try {
            FileHandler fileHandler = new FileHandler(tempFile.toString());
            ROOT_LOGGER.addHandler(fileHandler);
            
            assertThat("Precondition: a single handler in root logger",
                    ROOT_LOGGER.getHandlers().length, is(1));
            assertThat("Precondition: previous handler is FileHandler",
                    ROOT_LOGGER.getHandlers()[0], instanceOf(FileHandler.class));
            
            LoggingSetup.init();
            
            assertThat("Postcondition: a single handler in root logger",
                    ROOT_LOGGER.getHandlers().length, is(1));
            
            Handler handler = ROOT_LOGGER.getHandlers()[0];
            assertThat("Postcondition: Handler is StreamHandler",
                    handler, is(instanceOf(StreamHandler.class)));
            
            fileHandler.close();
            
        } finally {
            Files.delete(tempFile);
            assertThat("Cleanup: temporary log file is removed",
                    Files.isRegularFile(tempFile), is(false));
        }
    }
    
    @Test
    public void levelSetting() throws IOException {
        try {
            LoggingSetup.setLevel("INFO");
            assertThat("Postcondition: Logger should have correct level set",
                    ROOT_LOGGER.getLevel(), is(Level.INFO));
            
            LoggingSetup.setLevel("FINER");
            assertThat("Postcondition: Logger should have correct level set",
                    ROOT_LOGGER.getLevel(), is(Level.FINER));
            
            LoggingSetup.setLevel("WARNING");
            assertThat("Postcondition: Logger should have correct level set",
                    ROOT_LOGGER.getLevel(), is(Level.WARNING));
            
            LoggingSetup.setLevel("doesnt exist");
            assertThat("Postcondition: Logger should ignore invalid level",
                    ROOT_LOGGER.getLevel(), is(Level.WARNING));
            
        } finally {
            // clean up to default state
            LoggingSetup.setLevel("INFO");
        }
    }
    
    @Test
    public void addFileLogging() throws IOException {
        Path tempFile = Files.createTempFile("exercise-submitter-server-logging-test", ".log");
        assertThat("Precondition: temporary log file exists",
                Files.isRegularFile(tempFile), is(true));
        
        Handler handler = null;
        
        try {
            assertThat("Precondition: no handlers in root logger",
                    ROOT_LOGGER.getHandlers().length, is(0));
            
            LoggingSetup.addFileLogging(tempFile);
            
            assertThat("Postcondition: a single handler in root logger",
                    ROOT_LOGGER.getHandlers().length, is(1));
            
            handler = ROOT_LOGGER.getHandlers()[0];
            assertThat("Postcondition: Handler is FileHandler",
                    handler, is(instanceOf(FileHandler.class)));
            
            assertThat("Postcondition: custom formatter is set",
                    handler.getFormatter(), instanceOf(SingleLineLogFormatter.class));
            
            assertThat("Postcondition: UTF-8 encoding ist set",
                    handler.getEncoding(), is("UTF-8"));
            
            assertThat("Postcondition: Handler level is set to ALL",
                    handler.getLevel(), is(Level.ALL));
            
            assertThat("Postcondition: Logger level is set to INFO",
                    ROOT_LOGGER.getLevel(), is(Level.INFO));
            
            
        } finally {
            if (handler != null) {
                handler.close();
            }
            Files.delete(tempFile);
        }
    }
    
    @Test
    public void fileLoggingAppends() throws IOException {
        assertThat("Precondition: no handlers in root logger",
                ROOT_LOGGER.getHandlers().length, is(0));
        
        Path tempFile = Files.createTempFile("exercise-submitter-server-logging-test", ".log");
        assertThat("Precondition: temporary log file exists",
                Files.isRegularFile(tempFile), is(true));
        
        try {
            Files.writeString(tempFile, "content before\n", StandardCharsets.UTF_8);
            
            LoggingSetup.addFileLogging(tempFile);
            
            ROOT_LOGGER.log(Level.INFO, "log message");
            
            ROOT_LOGGER.getHandlers()[0].close();
            
            assertThat("Postcondition: previous content still exists",
                    Files.readAllLines(tempFile, StandardCharsets.UTF_8).get(0), is("content before"));
            
        } finally {
            Files.delete(tempFile);
            assertThat("Cleanup: temporary log file is removed",
                    Files.isRegularFile(tempFile), is(false));
        }
    }
    
}
