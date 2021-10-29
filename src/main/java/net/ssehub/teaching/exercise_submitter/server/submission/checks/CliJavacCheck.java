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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

/**
 * Implements a {@link JavacCheck} that uses standard command-line process <code>javac</code> to run the Java compiler.
 *  
 * @author Adam
 */
public class CliJavacCheck extends JavacCheck {

    private static final Logger LOGGER = Logger.getLogger(CliJavacCheck.class.getName());
    
    private static final Pattern JAVAC_OUTPUT_PATTERN = Pattern.compile(
            "^(?<filename>.+):(?<line>\\d+): (?<type>error|warning): (?<message>.+)$");

    private String javacCommand;
    
    /**
     * Creates a re-usable {@link CliJavacCheck} with the standard <code>javac</code> command.
     */
    public CliJavacCheck() {
        this.javacCommand = "javac";
    }
    
    /**
     * Sets the executable to run as the Java compiler. By default, this is <code>javac</code>. May be either an
     * absolute path to a binary or it must refer to a binary in the <code>PATH</code>.
     * 
     * @param javacCommand The executable to run as the java compiler.
     */
    public void setJavacCommand(String javacCommand) {
        this.javacCommand = javacCommand;
    }
    
    /**
     * Returns the configured value for this setting.
     * 
     * @return The configured Java compiler command.
     * 
     * @see #setJavacCommand(String)
     */
    public String getJavacCommand() {
        return javacCommand;
    }
    
    @Override
    protected boolean runJavac(Path submissionDirectory, Set<Path> javaFiles) {
        boolean success;
        
        ProcessBuilder processBuilder = new ProcessBuilder(buildCommand(submissionDirectory, javaFiles));
        processBuilder.redirectOutput(Redirect.DISCARD);
        processBuilder.redirectError(Redirect.PIPE);
        processBuilder.directory(submissionDirectory.toFile());
        
        LOGGER.log(Level.FINE, "Running {0} in directory {1}...", new Object[] {
            processBuilder.command(), submissionDirectory});
        
        try {
            Process process = processBuilder.start();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            List<String> output = new LinkedList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
            }
            
            int exitCode = process.waitFor();
            success = exitCode == 0;
            
            int numCreated = createMessagesFromOutput(output);
            
            if (!success && numCreated == 0) {
                addResultMessage(new ResultMessage(CHECK_NAME, MessageType.ERROR, "javac failed without message"));
            }
            
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "Exception while running javac", e);
            
            success = false;
            addResultMessage(new ResultMessage(CHECK_NAME, MessageType.ERROR,
                    "An internal error occurred while running javac"));
        }
        
        return success;
    }
    
    /**
     * Creates the full command for running the Java compiler on the given source files. Includes all settings
     * (e.g. Java version and file encoding). The command is expected to run with the given directory as the working
     * directory.
     * 
     * @param submissionDirectory The submission directory that contains all the Java file.
     * @param filesToCompile The set of files to compile. File paths are considered relative to current working
     *      directory, <b>not</b> relative to <code>directory</code>.
     * 
     * @return The command that runs the Java compiler on the given files.
     */
    private List<String> buildCommand(Path submissionDirectory, Set<Path> filesToCompile) {
        List<String> command = new LinkedList<>();
        command.add(javacCommand);
        
        command.add("-encoding");
        command.add(getCharset().name());
        
        command.add("--release");
        command.add(String.valueOf(getJavaVersion()));
        
        if (getEnableWarnings()) {
            command.add("-Xlint");
        }
        
        if (!getClasspath().isEmpty()) {
            command.add("--class-path");
            
            StringJoiner classpath = new StringJoiner(FileSystems.getDefault().getSeparator());
            for (Path classpathEntry : getClasspath()) {
                classpath.add(classpathEntry.toAbsolutePath().toString());
            }
            
            command.add(classpath.toString());
        }
        
        for (Path javaSourceFile : filesToCompile) {
            command.add(submissionDirectory.relativize(javaSourceFile).toString());
        }
        
        return command;
    }
    
    /**
     * Parses the output of the <code>javac</code> command and creates {@link ResultMessage} accordingly.
     * 
     * @param output The output lines of the Java compiler.
     * 
     * @return The number of created {@link ResultMessage}s.
     */
    private int createMessagesFromOutput(List<String> output) {
        int numCreated = 0;
        
        for (int i = 0; i < output.size(); i++) {
            String line = output.get(i);
            
            Matcher matcher = JAVAC_OUTPUT_PATTERN.matcher(line);
            if (matcher.matches()) {
                MessageType type = MessageType.valueOf(matcher.group("type").toUpperCase());
                ResultMessage message = new ResultMessage(CHECK_NAME, type, matcher.group("message"));
                
                message.setFile(Path.of(matcher.group("filename")));
                message.setLine(Integer.parseInt(matcher.group("line")));
                
                if (output.size() > i + 2) {
                    String caretLine = output.get(i + 2);
                    if (caretLine.trim().equals("^")) {
                        message.setColumn(caretLine.indexOf('^') + 1);
                        i += 2;
                    }
                }
                
                addResultMessage(message);
                numCreated++;
            }
        }
        
        return numCreated;
    }
    
}
