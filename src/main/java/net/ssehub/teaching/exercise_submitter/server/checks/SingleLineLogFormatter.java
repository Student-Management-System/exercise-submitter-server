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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A {@link Formatter} that prints {@link LogRecord}s as (mostly) single lines.
 * 
 * @author Adam
 */
class SingleLineLogFormatter extends Formatter {

    private static final DateTimeFormatter TIME_FORMATTER
            = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT).withZone(ZoneId.systemDefault());
    
    @Override
    public String format(LogRecord record) {
        StringBuilder logLine = new StringBuilder();
        
        
        logLine.append('[').append(TIME_FORMATTER.format(record.getInstant())).append("] ");
        logLine.append('[').append(record.getLevel().getName()).append("] ");
        
        String className = null;
        if (record.getSourceClassName() != null) {
            className = shortenClassName(record.getSourceClassName());
            if (record.getSourceMethodName() != null) {
                className += '.' + record.getSourceMethodName();
            }
            
        } else if (record.getLoggerName() != null) {
            className = shortenClassName(record.getLoggerName());
        }
        
        if (className != null) {
            logLine.append('[').append(className).append("] ");
        }
        
        int prefixLength = logLine.length();
        
        logLine.append(formatMessage(record));
        logLine.append('\n');
        
        if (record.getThrown() != null) {
            logException(logLine, prefixLength, record.getThrown());
        }
        
        return logLine.toString();
    }
    
    /**
     * Converts a given exception to a string and adds it to the log output.
     * 
     * @param logBuffer The log output to add the lines to.
     * @param prefixLength The number of whitespace characters to add in front of each line.
     * @param exception The exception to convert into a string.
     */
    private static void logException(StringBuilder logBuffer, int prefixLength, Throwable exception) {
        String prefix = " ".repeat(prefixLength);
        
        logBuffer.append(prefix).append(exception.getClass().getName());
        if (exception.getMessage() != null) {
            logBuffer.append(": ").append(exception.getMessage());
        }
        logBuffer.append('\n');
        
        StackTraceElement[] stack = exception.getStackTrace();
        for (StackTraceElement stackElement : stack) {
            logBuffer.append(prefix).append("  at ").append(stackElement.toString()).append('\n');
        }
        
        Throwable cause = exception.getCause();
        if (cause != null) {
            logBuffer.append(prefix).append("Caused by:").append('\n');
            logException(logBuffer, prefixLength, cause);
        }
    }
    
    /**
     * Returns only the last part of a given fully-qualified class name.
     * 
     * @param className The fully qualified class name to shorten.
     * 
     * @return The part after the last dot.
     */
    private static String shortenClassName(String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot != -1) {
            className = className.substring(lastDot + 1);
        }
        return className;
    }

}
