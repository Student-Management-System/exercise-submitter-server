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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

/**
 * A utility class for setting up the {@link Logger}. 
 * 
 * @author Adam
 */
public class LoggingSetup {

    private static final Logger LOGGER = Logger.getLogger(LoggingSetup.class.getName());
    
    private static final Logger GLOBAL_ROOT_LOGGER = Logger.getLogger("");
    
    private static final Logger OUR_ROOT_LOGGER = Logger.getLogger("net.ssehub.teaching.submission_check");
    
    private static Level level = Level.INFO;
    
    /**
     * Don't allow any instances.
     */
    private LoggingSetup() {}
    
    /**
     * Sets the global logging {@link Level}. This affects the current configuration as well as all future
     * setup*() method calls.
     * 
     * @param level The new level as a string. If this is invalid, the level is not changed.
     * 
     * @see Level#parse(String)
     */
    public static void setLevel(String level) {
        try {
            LoggingSetup.level = Level.parse(level);
            OUR_ROOT_LOGGER.setLevel(LoggingSetup.level);
            
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }
    
    /**
     * Sets up the logging to log to a given log-file. If logging to file fails, {@link #setupStdoutLogging()} is used
     * instead as a fallback.
     * 
     * @param logfile The file to log to.
     */
    public static final void setupFileLogging(File logfile) {
        removeDefaultLogging();
        
        try {
            FileHandler handler = new FileHandler(logfile.getPath(), true);
            handler.setFormatter(new SingleLineLogFormatter());
            handler.setLevel(Level.ALL);
            try {
                handler.setEncoding("UTF-8");
            } catch (UnsupportedEncodingException e) {
                // can't happen, ignore
            }
            
            OUR_ROOT_LOGGER.addHandler(handler);
            OUR_ROOT_LOGGER.setLevel(level);
            
            Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionLogger());
            
        } catch (IOException e) {
            setupStdoutLogging();
            LOGGER.log(Level.SEVERE, "Failed to create log file, logging to console instead", e);
        }
    }
    
    /**
     * Sets up the logging to log to standard output.
     */
    public static final void setupStdoutLogging() {
        removeDefaultLogging();
        
        StreamHandler handler = new StreamHandler(System.out, new SingleLineLogFormatter()) {
            
            @Override
            public synchronized void close() {
                // only flush, so we don't close System.out
                flush();
            }
            
        };
        handler.setLevel(Level.ALL);
        try {
            handler.setEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // can't happen, ignore
        }
        
        OUR_ROOT_LOGGER.addHandler(handler);
        OUR_ROOT_LOGGER.setLevel(level);
        
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionLogger());
    }
    
    /**
     * Removes all handlers from the root logger.
     */
    private static void removeDefaultLogging() {
        for (Handler handler : GLOBAL_ROOT_LOGGER.getHandlers()) {
            GLOBAL_ROOT_LOGGER.removeHandler(handler);
            handler.close();
        }
        for (Handler handler : OUR_ROOT_LOGGER.getHandlers()) {
            OUR_ROOT_LOGGER.removeHandler(handler);
            handler.close();
        }
    }
    
    /**
     * An uncaught exception handler that logs exceptions.
     */
    private static class UncaughtExceptionLogger implements UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread thread, Throwable exception) {
            OUR_ROOT_LOGGER.log(Level.SEVERE, "Uncaught exception in thread " + thread.getName(), exception);
        }
        
    }

}
