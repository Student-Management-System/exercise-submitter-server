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
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

/**
 * Checks that file-size restrictions are not violated. Fails if any file is larger than {@link #setMaxFileSize(long)}
 * or if the whole submission is larger than {@link #setMaxSubmissionSize(long)}. Creates appropriate
 * {@link ResultMessage}s for all files (or the whole submission) that are too large.
 * 
 * @author Adma
 */
public class FileSizeCheck extends Check {
    
    public static final String CHECK_NAME = "file-size";
    
    private static final Logger LOGGER = Logger.getLogger(FileSizeCheck.class.getName());
    
    private long maxFileSize;
    
    private long maxSubmissionSize;
    
    /**
     * Creates a re-usable {@link FileSizeCheck} with 10 MiB as the limit for both, the single file and the overall
     * submission size.
     */
    public FileSizeCheck() {
        this.maxFileSize = 1024 * 1024 * 10;
        this.maxSubmissionSize = 1024 * 1024 * 10;
    }
    
    /**
     * Sets the maximum number of bytes a single file may have.
     * 
     * @param maxFileSize The maximum size of a single file.
     */
    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }
    
    /**
     * Sets the maximum number of bytes that all files of a submission may have combined.
     * 
     * @param maxSubmissionSize The maximum total submission size.
     */
    public void setMaxSubmissionSize(long maxSubmissionSize) {
        this.maxSubmissionSize = maxSubmissionSize;
    }
    
    /**
     * Returns the configured maximum size for a single file.
     * 
     * @return The maximum size in bytes.
     * 
     * @see #setMaxFileSize(long)
     */
    public long getMaxFileSize() {
        return maxFileSize;
    }
    
    /**
     * Returns the configured maximum size for the whole submission.
     * 
     * @return The maximum size in bytes.
     * 
     * @see #setMaxSubmissionSize(long)
     */
    public long getMaxSubmissionSize() {
        return maxSubmissionSize;
    }


    @Override
    public boolean run(File submissionDirectory) {
        int numErrors = 0;
        long submissionSize = 0;
        
        try {
            for (File file : FileUtils.findAllFiles(submissionDirectory)) {
                long fileSize = FileUtils.getFileSize(file);
                
                LOGGER.log(Level.FINE, "File {0} has size of {1} bytes", new Object[] {
                    file, fileSize});
                
                submissionSize += fileSize;
                
                if (fileSize > this.maxFileSize) {
                    numErrors++;
                    
                    ResultMessage message = new ResultMessage(CHECK_NAME, MessageType.ERROR, "File is too large");
                    message.setFile(FileUtils.getRelativeFile(submissionDirectory, file));
                    addResultMessage(message);
                }
                
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Exception while checking file-size", e);
            
            numErrors++;
            addResultMessage(new ResultMessage(CHECK_NAME, MessageType.ERROR,
                    "An internal error occurred while checking file-sizes"));
        }
        
        LOGGER.log(Level.FINE, "Submission has total size of {0} bytes", submissionSize);
        
        if (submissionSize > this.maxSubmissionSize) {
            numErrors++;
            addResultMessage(new ResultMessage(CHECK_NAME, MessageType.ERROR, "Submission size is too large"));
        }
        
        return numErrors == 0;
    }
    
}
