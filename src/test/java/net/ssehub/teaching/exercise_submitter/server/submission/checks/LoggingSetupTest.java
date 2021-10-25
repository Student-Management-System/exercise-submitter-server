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

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

public class LoggingSetupTest {

    public static final Logger ROOT_LOGGER = Logger.getLogger("net.ssehub.teaching.submission_check");
    
    private static final File TESTDATA = new File("src/test/resources");
    
    @Test
    public void fileLogging() throws IOException {
        UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(null);
        try {
            assertThat("Precondition: no handlers in root logger",
                    ROOT_LOGGER.getHandlers().length, is(0));
            
            assertThat("Precondition: no uncaught exception handler",
                    Thread.getDefaultUncaughtExceptionHandler(), is(nullValue()));
            
            File out = File.createTempFile("submission-check-logging-test", ".log");
            assertThat("Precondition: temporary log file exists",
                    out.isFile(), is(true));
            
            LoggingSetup.setupFileLogging(out);
            
            assertThat("Postcondition: a single handler in root logger",
                    ROOT_LOGGER.getHandlers().length, is(1));
            
            Handler handler = ROOT_LOGGER.getHandlers()[0];
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
            
            assertThat("Postcondition: uncaught exception handler registered",
                    Thread.getDefaultUncaughtExceptionHandler(), not(nullValue()));
            
            handler.close();
            
            out.delete();
            assertThat("Cleanup: temporary log file is removed",
                    out.exists(), is(false));
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(previousHandler);
        }
        
    }
    
    @Test
    public void fileLoggingAppends() throws IOException {
        assertThat("Precondition: no handlers in root logger",
                ROOT_LOGGER.getHandlers().length, is(0));
        
        File out = File.createTempFile("submission-check-logging-test", ".log");
        assertThat("Precondition: temporary log file exists",
                out.isFile(), is(true));
        
        try (FileWriter writer = new FileWriter(out)) {
            writer.write("content before\n");
        }
        
        LoggingSetup.setupFileLogging(out);
        
        ROOT_LOGGER.log(Level.INFO, "log message");
        
        ROOT_LOGGER.getHandlers()[0].close();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(out))) {
            assertThat("Postcondition: previous content still exists",
                    reader.readLine(), is("content before"));
        }
        
        out.delete();
        assertThat("Cleanup: temporary log file is removed",
                out.exists(), is(false));
    }
    
    @Test
    public void stdoutLogging() throws IOException {
        PrintStream oldOut = System.out;
        
        UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(null);
        
        try {
            assertThat("Precondition: no handlers in root logger",
                    ROOT_LOGGER.getHandlers().length, is(0));
            
            assertThat("Precondition: no uncaught exception handler",
                    Thread.getDefaultUncaughtExceptionHandler(), is(nullValue()));
            
            ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
            
            System.setOut(new PrintStream(stdoutBuffer));
            
            LoggingSetup.setupStdoutLogging();
            
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
            
            assertThat("Postcondition: uncaught exception handler registered",
                    Thread.getDefaultUncaughtExceptionHandler(), not(nullValue()));
            
            ROOT_LOGGER.log(Level.INFO, "some test log message");
            
            handler.flush();
            
            assertThat("Postcondition: log messages should appear in stdout",
                    stdoutBuffer.toString().contains("some test log message"), is(true));
            
        } finally {
            System.setOut(oldOut);
            Thread.setDefaultUncaughtExceptionHandler(previousHandler);
        }
    }
    
    @Test
    public void uncaughtExceptionLogger() throws IOException, InterruptedException {
        PrintStream oldOut = System.out;
        
        UncaughtExceptionHandler previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(null);
        
        try {
            assertThat("Precondition: no uncaught exception handler",
                    Thread.getDefaultUncaughtExceptionHandler(), is(nullValue()));
            
            ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
            
            System.setOut(new PrintStream(stdoutBuffer));
            
            LoggingSetup.setupStdoutLogging();
            
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
            Thread.setDefaultUncaughtExceptionHandler(previousHandler);
        }
    }
    
    @Test
    @Tag("pitest-ignore")
    @Disabled("TODO: find out why this test fails") // TODO: refactor logging anyway
    public void fileLoggingFallsBackToStdoutLogging() throws IOException {
        PrintStream oldOut = System.out;
        
        try {
            assertThat("Precondition: no handlers in root logger",
                    ROOT_LOGGER.getHandlers().length, is(0));
            
            ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
            
            System.setOut(new PrintStream(stdoutBuffer));
            
            assertThat("Precondition: testdata is a directory",
                    TESTDATA.isDirectory(), is(true));
            
            LoggingSetup.setupFileLogging(TESTDATA);
            
            assertThat("Postcondition: a single handler in root logger",
                    ROOT_LOGGER.getHandlers().length, is(1));
            
            Handler handler = ROOT_LOGGER.getHandlers()[0];
            assertThat("Postcondition: Handler is StreamHandler",
                    handler, is(instanceOf(StreamHandler.class)));
            
            handler.flush();
            
            assertThat("Postcondition: an error log message should appear in stdout",
                    stdoutBuffer.toString().contains("Failed to create log file, logging to console instead"), is(true));
            
        } finally {
            System.setOut(oldOut);
            
            // cleanup
            File lockFile = new File(TESTDATA.getParentFile(), TESTDATA.getName() + ".lck");
            if (lockFile.isFile()) {
                lockFile.delete();
            }
        }
    }
    
    @Test
    public void removesPreviousHandlers() throws IOException {
        assertThat("Precondition: no handlers in root logger",
                ROOT_LOGGER.getHandlers().length, is(0));
        
        File out = File.createTempFile("submission-check-logging-test", ".log");
        FileHandler fileHandler = new FileHandler(out.getPath());
        ROOT_LOGGER.addHandler(fileHandler);
        
        assertThat("Precondition: a single handler in root logger",
                ROOT_LOGGER.getHandlers().length, is(1));
        assertThat("Precondition: previous handler is FileHandler",
                ROOT_LOGGER.getHandlers()[0], instanceOf(FileHandler.class));
        
        LoggingSetup.setupStdoutLogging();
        
        assertThat("Postcondition: a single handler in root logger",
                ROOT_LOGGER.getHandlers().length, is(1));
        
        Handler handler = ROOT_LOGGER.getHandlers()[0];
        assertThat("Postcondition: Handler is StreamHandler",
                handler, is(instanceOf(StreamHandler.class)));
        
        fileHandler.close();
        out.delete();
        assertThat("Cleanup: temporary log file is removed",
                out.exists(), is(false));
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
    
    @BeforeEach
    public void removeRootLoggerHandlers() {
        for (Handler handler : ROOT_LOGGER.getHandlers()) {
            ROOT_LOGGER.removeHandler(handler);
        }
    }
    
}
