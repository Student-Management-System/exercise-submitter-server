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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
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
    
    private static Level level = Level.INFO;
    
    /**
     * Don't allow any instances.
     */
    private LoggingSetup() {}
    
    /**
     * Sets up the logging to log to standard output.
     */
    public static void init() {
        removeDefaultLogging();
        
        StreamHandler handler = new StreamHandler(System.out, new SingleLineLogFormatter()) {
         
            @Override
            public synchronized void publish(LogRecord record) {
                super.publish(record);
                // flush after each log entry
                flush();
            }
            
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
        
        GLOBAL_ROOT_LOGGER.addHandler(handler);
        GLOBAL_ROOT_LOGGER.setLevel(level);
        
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionLogger());
    }
    
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
            GLOBAL_ROOT_LOGGER.setLevel(LoggingSetup.level);
            
        } catch (IllegalArgumentException e) {
            // ignore
        }
    }
    
    /**
     * Sets up the logging to log to a given log-file.
     * 
     * @param logfile The file to log to.
     */
    public static void addFileLogging(Path logfile) {
        try {
            FileHandler handler = new FileHandler(logfile.toString(), true);
            handler.setFormatter(new SingleLineLogFormatter());
            handler.setLevel(Level.ALL);
            try {
                handler.setEncoding("UTF-8");
            } catch (UnsupportedEncodingException e) {
                // can't happen, ignore
            }
            
            GLOBAL_ROOT_LOGGER.addHandler(handler);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to open log file " + logfile, e);
        }
    }
    
    /**
     * Removes all handlers from the root logger.
     */
    private static void removeDefaultLogging() {
        for (Handler handler : GLOBAL_ROOT_LOGGER.getHandlers()) {
            GLOBAL_ROOT_LOGGER.removeHandler(handler);
            handler.close();
        }
    }
    
    /**
     * An uncaught exception handler that logs exceptions.
     */
    private static class UncaughtExceptionLogger implements UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread thread, Throwable exception) {
            GLOBAL_ROOT_LOGGER.log(Level.SEVERE, "Uncaught exception in thread " + thread.getName(), exception);
        }
        
    }

}
