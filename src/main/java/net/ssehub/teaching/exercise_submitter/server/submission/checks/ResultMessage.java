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

import java.io.File;
import java.util.Objects;

/**
 * A message containing a result of a {@link Check}. May contain information about files and line numbers. These
 * messages are sent back to the client by the {@link SubmissionHook}.
 * 
 * @author Adam
 */
public class ResultMessage implements Comparable<ResultMessage> {
    
    /**
     * The type of a message.
     */
    public enum MessageType {
        WARNING,
        ERROR;
    }
    
    private String checkName;
    
    private MessageType type;
    
    private String message;
    
    private File file;
    
    private Integer line;
    
    private Integer column;
    
    /**
     * Creates a new {@link ResultMessage} with a simple message.
     *  
     * @param checkName The name of the {@link Check} that created this message.
     * @param type The type of this message.
     * @param message The message describing the result of the check.
     */
    public ResultMessage(String checkName, MessageType type, String message) {
        this.checkName = checkName;
        this.type = type;
        this.message = message;
    }

    /**
     * Sets the file where this result occurred. This is a relative path inside the submission directory.
     * 
     * @param file The file where this result occurred.
     * 
     * @return this (for convenience)
     */
    public ResultMessage setFile(File file) {
        this.file = file;
        return this;
    }
    
    /**
     * Sets the line number in the file where this result occurred.
     * 
     * @param line The line number.
     * 
     * @return this (for convenience)
     */
    public ResultMessage setLine(int line) {
        this.line = line;
        return this;
    }
    
    /**
     * Sets the column number in the file where this result occurred.
     * 
     * @param column The column number.
     * 
     * @return this (for convenience)
     */
    public ResultMessage setColumn(int column) {
        this.column = column;
        return this;
    }
    
    /**
     * Returns the name of the {@link Check} that created this message.
     * 
     * @return The {@link Check} that created this message.
     */
    public String getCheckName() {
        return checkName;
    }

    /**
     * Returns the {@link MessageType} of this message.
     * 
     * @return The type of this message.
     */
    public MessageType getType() {
        return type;
    }
    
    /**
     * Returns the message string of this message.
     * 
     * @return The content of this message.
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Returns the file location that this message refers to. This is a relative path to the submission.
     * 
     * @return The file location; may be <code>null</code>.
     */
    public File getFile() {
        return file;
    }
    
    /**
     * Returns the line location of this message.
     * 
     * @return The line number; may be <code>null</code>.
     */
    public Integer getLine() {
        return line;
    }
    
    /**
     * Returns the column location of this message.
     * 
     * @return The column number; may be <code>null</code>.
     */
    public Integer getColumn() {
        return column;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(checkName, column, file, line, message, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ResultMessage)) {
            return false;
        }
        ResultMessage other = (ResultMessage) obj;
        return Objects.equals(checkName, other.checkName) && Objects.equals(column, other.column)
                && Objects.equals(file, other.file) && Objects.equals(line, other.line)
                && Objects.equals(message, other.message) && type == other.type;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        
        builder.append(checkName).append(' ').append(type.name().toLowerCase());
        
        if (file != null) {
            builder.append(" in ").append(file.getPath());
            if (line != null) {
                builder.append(':').append(line);
                if (column != null) {
                    builder.append(':').append(column);
                }
            }
        }
        
        builder.append(" \"").append(message).append('"');
        
        return builder.toString();
    }
    
    /**
     * Compares two (possibly <code>null</code>) integer values. <code>null</code> is less than any number.
     * 
     * @param i1 The left integer to compare.
     * @param i2 The right integer to compare.
     * 
     * @return -1 if left < right; 0 if left == right; +1 if left > right;
     */
    private int compareIntegers(Integer i1, Integer i2) {
        int result;
        if (i1 == null) {
            if (i2 == null) {
                result = 0;
            } else {
                result = -1;
            }
        } else {
            if (i2 == null) {
                result = 1;
            } else {
                result = Integer.compare(i1, i2);
            }
        }
        return result;
    }

    /**
     * Compares {@link ResultMessage}s so that they can be sorted according to check name and location.
     */
    @Override
    public int compareTo(ResultMessage other) {
        
        int result = this.checkName.compareTo(other.checkName);
        
        if (result == 0) {
            if (this.file != null && other.file != null) {
                result = this.file.compareTo(other.file);
                if (result == 0) {
                    result = compareIntegers(this.line, other.line);
                    if (result == 0) {
                        result = compareIntegers(this.column, other.column);
                    }
                }
                
            } else {
                if (this.file == null && other.file == null) {
                    result = 0;
                } else if (this.file == null) {
                    result = -1;
                } else {
                    result = 1;
                }
            }
        }
        
        return result;
    }
    
}
