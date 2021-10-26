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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

/**
 * Compiles all Java source files. Fails if any file does not compile. Creates {@link ResultMessage}s for all
 * compilation errors and warnings.
 * 
 * @author Adam
 */
public abstract class JavacCheck extends Check {
    
    public static final String CHECK_NAME = "javac";
    
    private static final Logger LOGGER = Logger.getLogger(JavacCheck.class.getName());
    
    private int javaVersion;
    
    private Charset charset;
    
    private boolean enableWarnings;
    
    private List<File> additionalClasspath;
    
    /**
     * Creates a re-usable {@link JavacCheck}.
     */
    public JavacCheck() {
        this.javaVersion = 11;
        this.charset = StandardCharsets.UTF_8;
        this.enableWarnings = false;
        this.additionalClasspath = new LinkedList<>();
    }
    
    /**
     * Sets the Java version to use when compiling source files. By default, this is <code>11</code>. This is passed
     * via the <code>--release</code> flag to the Java compiler.
     * 
     * @param javaVersion The Java version to use when compiling source files.
     */
    public void setJavaVersion(int javaVersion) {
        this.javaVersion = javaVersion;
    }
    
    /**
     * Sets the {@link Charset} (encoding) to use when compiling source files. By default, this is
     * {@link StandardCharsets#UTF_8}. This is passed via the <code>-encoding</code> flag to the Java compiler.
     * 
     * @param charset The charset that source files are encoded in.
     */
    public void setCharset(Charset charset) {
        this.charset = charset;
    }
    
    /**
     * Sets whether the compiler should enable all warning messages. By default, this is <code>false</code>. If
     * <code>true</code>, <code>-Xlint</code> is passed to the Java compiler.
     * 
     * @param enableWarnings Whether warnings should be enabled.
     */
    public void setEnableWarnings(boolean enableWarnings) {
        this.enableWarnings = enableWarnings;
    }
    
    /**
     * Adds the given file to the classpath for compilation. All files passed to this method are passed via
     * <code>--class-path</code> to the Java compiler.
     * 
     * @param classpathEntry The entry to pass as the classpath. May be either a directory or a jar file.
     */
    public void addToClasspath(File classpathEntry) {
        this.additionalClasspath.add(classpathEntry);
    }
    
    /**
     * Clears all previous entries added via {@link #addToClasspath(File)}.
     */
    public void clearClasspath() {
        this.additionalClasspath.clear();
    }
    
    /**
     * Returns the configured value for this setting.
     * 
     * @return The configured Java version.
     * 
     * @see #setJavaVersion(int)
     */
    public int getJavaVersion() {
        return javaVersion;
    }
    
    /**
     * Returns the configured value for this setting.
     * 
     * @return The configured charset.
     * 
     * @see #setCharset(Charset)
     */
    public Charset getCharset() {
        return charset;
    }
    
    /**
     * Returns the configured value for this setting.
     * 
     * @return Whether warnings are enabled.
     * 
     * @see #setEnableWarnings(boolean)
     */
    public boolean getEnableWarnings() {
        return enableWarnings;
    }
    
    /**
     * Returns the configured value for this setting.
     * 
     * @return An unmodifiable view on the configured classpath.
     * 
     * @see #addToClasspath(File)
     * @see #clearClasspath()
     */
    public List<File> getClasspath() {
        return Collections.unmodifiableList(additionalClasspath);
    }
    
    @Override
    public boolean run(File submissionDirectory) {
        boolean success;
        
        Set<File> javaFiles = FileUtils.findFilesBySuffix(submissionDirectory, ".java");
        
        if (!javaFiles.isEmpty()) {
            LOGGER.log(Level.FINE, "Compiling files {0}...", javaFiles);
            success = runJavac(submissionDirectory, javaFiles);
            
        } else {
            success = false;
            addResultMessage(new ResultMessage(CHECK_NAME, MessageType.ERROR, "No Java files found"));
        }
        
        return success;
    }
    
    /**
     * Runs the Java compiler on the given Java source files in the given directory.
     * 
     * @param submissionDirectory The directory to work in.
     * @param javaFiles All Java source files in that directory.
     * 
     * @return Whether the compilation was successful.
     */
    protected abstract boolean runJavac(File submissionDirectory, Set<File> javaFiles);

}
