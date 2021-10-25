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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

/**
 * Checks that all text files in the submission have a valid encoding. Fails if any text file cannot be decoded with the
 * required encoding. Creates {@link ResultMessage}s for each such incorrectly encoded file. Which files are text files
 * is determined via {@link Files#probeContentType(java.nio.file.Path)}.
 * 
 * @author Adam
 */
public class EncodingCheck extends Check {
    
    public static final String CHECK_NAME = "encoding";
    
    private static final Logger LOGGER = Logger.getLogger(EncodingCheck.class.getName());
    
    private Charset wantedCharset;
    
    /**
     * Creates a re-usable {@link EncodingCheck} with UTF-8 as the required charset (encoding).
     */
    public EncodingCheck() {
        this.wantedCharset = StandardCharsets.UTF_8;
    }
    
    /**
     * Sets the charset (encoding) that is required for all submission files.
     * 
     * @param wantedCharset The required {@link Charset}.
     */
    public void setWantedCharset(Charset wantedCharset) {
        this.wantedCharset = wantedCharset;
    }
    
    /**
     * Returns the charset (encoding) that is required for all submission files.
     * 
     * @return The {@link Charset} that is configured for this check.
     */
    public Charset getWantedCharset() {
        return wantedCharset;
    }

    @Override
    public boolean run(File submissionDirectory) {
        boolean success = true;
        
        try {
            for (File file : FileUtils.findAllFiles(submissionDirectory)) {
                String mimeType = Files.probeContentType(file.toPath());
                if (mimeType != null && mimeType.startsWith("text")) {
                    LOGGER.log(Level.FINER, "Checking file {0}...", file);
                    success &= checkFile(file, submissionDirectory);
                } else {
                    LOGGER.log(Level.FINER, "Skipping file {0} with non-text mime-type {1}", new Object[] {
                        file, mimeType});
                }
            }
            
        }  catch (IOException e) {
            LOGGER.log(Level.WARNING, "Exception while checking encoding", e);
            
            success = false;
            addResultMessage(new ResultMessage(CHECK_NAME, MessageType.ERROR,
                    "An internal error occurred while checking file encoding"));
        }
        
        return success;
    }

    /**
     * Checks the encoding of a single file. Creates and adds a {@link ResultMessage} if the file has a wrong encoding. 
     * 
     * @param file The file to check.
     * @param submissionDirectory The submission directory where this file comes from.
     *  
     * @return Whether this file has the correct encoding.
     * 
     * @throws IOException If reading the file fails.
     */
    private boolean checkFile(File file, File submissionDirectory) throws IOException {
        boolean result;
        
        CharsetDecoder decoder = wantedCharset.newDecoder();
        
        try (InputStream input = FileUtils.newInputStream(file)) {
            ByteBuffer buffer = ByteBuffer.wrap(input.readAllBytes());
            
            decoder.decode(buffer);
            
            result = true;
            
        } catch (CharacterCodingException e) {
            result = false;
            
            ResultMessage resultMessage = new ResultMessage(CHECK_NAME, MessageType.ERROR,
                    "File has invalid encoding; expected " + wantedCharset.displayName());
            resultMessage.setFile(FileUtils.getRelativeFile(submissionDirectory, file));
            
            addResultMessage(resultMessage);
        }
        
        return result;
    }

}
