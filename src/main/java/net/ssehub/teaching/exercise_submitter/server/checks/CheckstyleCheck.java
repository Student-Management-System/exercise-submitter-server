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
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;

import net.ssehub.teaching.exercise_submitter.server.checks.ResultMessage.MessageType;

/**
 * Runs Checkstyle on all Java source files. Fails if there are any Checkstyle errors. Creates {@link ResultMessage}s
 * for all Checkstyle errors and warnings.
 * 
 * @author Adam
 */
public class CheckstyleCheck extends Check {
    
    public static final String CHECK_NAME = "checkstyle";
    
    private static final Logger LOGGER = Logger.getLogger(CheckstyleCheck.class.getName());
    
    private File checkstyleRules;

    private Charset charset;
    
    /**
     * Creates a re-usable {@link CheckstyleCheck}.
     * 
     * @param checkstyleRules A file with the XML ruleset for Checkstyle.
     */
    public CheckstyleCheck(File checkstyleRules) {
        this.checkstyleRules = checkstyleRules;
        this.charset = StandardCharsets.UTF_8;
    }

    /**
     * Sets the charset (encoding) that Checkstyle should use to parse the files. By default, this is
     * {@link StandardCharsets#UTF_8}.
     * 
     * @param charset The charset to use.
     */
    public void setCharset(Charset charset) {
        this.charset = charset;
    }
    
    /**
     * Returns the Checkstyle rules file that is configured for this check.
     * 
     * @return The configured Checkstyle rules file.
     */
    public File getCheckstyleRules() {
        return checkstyleRules;
    }
    
    /**
     * Returns the charset (encoding) that is configured for this check.
     * 
     * @return The charset that is set.
     * 
     * @see #setCharset(Charset)
     */
    public Charset getCharset() {
        return charset;
    }
    
    @Override
    public boolean run(File submissionDirectory) {
        boolean success;
        
        Set<File> javaFiles = FileUtils.findFilesBySuffix(submissionDirectory, ".java");
        
        if (!javaFiles.isEmpty()) {
            success = runCheckstyle(submissionDirectory, javaFiles);
            
        } else {
            success = true;
        }
        
        return success;
    }

    /**
     * Runs Checkstyle on the given Java source files in the given directory.
     * 
     * @param submissionDirectory The directory to work in.
     * @param javaFiles All Java source files in that directory.
     * 
     * @return Whether the check was successful.
     */
    private boolean runCheckstyle(File submissionDirectory, Set<File> javaFiles) {
        boolean success;
        
        LOGGER.log(Level.FINER, "Using rules: {0}", checkstyleRules);
        LOGGER.log(Level.FINER, "Running on files: {0}...", javaFiles);
        
        try {
            Configuration configuration = ConfigurationLoader.loadConfiguration(
                    checkstyleRules.getAbsolutePath(), null);
            
            Checker checkstyle = new Checker();
            checkstyle.setModuleClassLoader(Checker.class.getClassLoader());
            checkstyle.configure(configuration);
            checkstyle.setBasedir(submissionDirectory.getAbsolutePath());
            checkstyle.setCharset(this.charset.name());
            checkstyle.setHaltOnException(false);
            
            CheckstyleOutputListener listener = new CheckstyleOutputListener();
            checkstyle.addListener(listener);
            
            checkstyle.process(new ArrayList<>(javaFiles));
            checkstyle.destroy();
            
            success = listener.getNumErrors() == 0;
            
        } catch (CheckstyleException | UnsupportedEncodingException e) {
            LOGGER.log(Level.WARNING, "Exception while running Checkstyle", e);
            
            addResultMessage(new ResultMessage(CHECK_NAME, MessageType.ERROR,
                    "An internal error occurred while running Checkstyle"));
            success = false;
        }
        
        return success;
    }
    
    /**
     * A listener that processes the audit events that Checkstyle produces.
     */
    private class CheckstyleOutputListener implements AuditListener {

        /**
         * For some reason, {@link #addException(AuditEvent, Throwable)} is not called for parsing
         * errors. Instead, {@link #addError(AuditEvent)} is called with the exception in the
         * error message. This pattern detects this so that the exception message can be modified accordingly.
         */
        private final Pattern parsingExceptionPattern = Pattern.compile(
            "^Got an exception - com\\.puppycrawl\\.tools\\.checkstyle\\.api\\.CheckstyleException: (?<exception>.+) "
            + "occurred while parsing file (?<filename>.+)\\.$", Pattern.MULTILINE);
        
        private int numErrors = 0;
        
        /**
         * Returns the number of errors that occurred during the audit.
         * 
         * @return The number of error messages created.
         */
        public int getNumErrors() {
            return numErrors;
        }
        
        @Override
        public void auditStarted(AuditEvent event) {
        }

        @Override
        public void auditFinished(AuditEvent event) {
        }

        @Override
        public void fileStarted(AuditEvent event) {
        }

        @Override
        public void fileFinished(AuditEvent event) {
        }

        @Override
        public void addError(AuditEvent event) {
            MessageType type;
            
            switch (event.getSeverityLevel()) {
            case WARNING:
                type = MessageType.WARNING;
                break;
                
            case ERROR:
                type = MessageType.ERROR;
                numErrors++;
                break;
                
            default:
                type = null;
                break;
            }
            
            if (type != null) {
                String message = event.getMessage();
                int line = event.getLine();
                int column = event.getColumn();
                
                Matcher matcher = parsingExceptionPattern.matcher(event.getMessage());
                if (matcher.find()) {
                    message = "Checkstyle could not parse file";
                    line = 0;
                }
                if (message.endsWith(".")) {
                    message = message.substring(0, message.length() - 1);
                }
                
                ResultMessage resultMessage = new ResultMessage(CHECK_NAME, type, message);
                
                if (event.getFileName() != null) {
                    resultMessage.setFile(new File(event.getFileName()));
                }
                if (line != 0) {
                    resultMessage.setLine(line);
                }
                if (column != 0) {
                    resultMessage.setColumn(column);
                }
                
                addResultMessage(resultMessage);
            }
        }

        @Override
        public void addException(AuditEvent event, Throwable throwable) {
            // for some reason this method is not called on parsing exceptions
        }
        
    }
    
}
