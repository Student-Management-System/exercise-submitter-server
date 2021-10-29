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

import java.io.Writer;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

/**
 * A {@link JavacCheck} that uses the internal {@link JavaCompiler} interface. Use {@link #isSupported()} to check
 * if the current runtime supports the internal {@link JavaCompiler}.
 * 
 * @author Adam
 */
public class InternalJavacCheck extends JavacCheck {
    
    private static final Logger LOGGER = Logger.getLogger(InternalJavacCheck.class.getName());

    /**
     * Checks whether the internal compiler is supported by this runtime.
     * 
     * @return Whether this compiler can be used.
     */
    public static boolean isSupported() {
        return ToolProvider.getSystemJavaCompiler() != null;
    }
    
    @Override
    protected boolean runJavac(Path submissionDirectory, Set<Path> javaFiles) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(
                new DiagnosticCollector<>(), // discard error messages from FileManager
                Locale.ROOT, getCharset());
        Iterable<? extends JavaFileObject> javaFileObjects
                = fileManager.getJavaFileObjects(javaFiles.toArray(new Path[0]));
        
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        
        boolean success;
        try {
            CompilationTask task = compiler.getTask(
                    Writer.nullWriter(), // discard additional output
                    fileManager,
                    diagnosticCollector,
                    buildOptions(),
                    null, // no additional classes for annotation processing
                    javaFileObjects
            );
    
            success = task.call();
            
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Exception while setting up compilaton task", e);
            
            success = false;
            addResultMessage(new ResultMessage(CHECK_NAME, MessageType.ERROR,
                    "An internal error occurred while running javac"));
        }
        
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics()) {
            convertDiagnostToResultMessage(diagnostic, submissionDirectory);
        }
        
        return success;
    }
    
    /**
     * Builds the options to pass to the compiler.
     * 
     * @return A list of options.
     */
    private List<String> buildOptions() {
        List<String> options = new LinkedList<>();
        
        options.add("--release");
        options.add(String.valueOf(getJavaVersion()));
        
        if (getEnableWarnings()) {
            options.add("-Xlint");
        }
        
        // disable warnings about possibly deprecated options
        options.add("-Xlint:-options");
        
        if (!getClasspath().isEmpty()) {
            options.add("--class-path");
            
            StringJoiner classpath = new StringJoiner(FileSystems.getDefault().getSeparator());
            for (Path classpathEntry : getClasspath()) {
                classpath.add(classpathEntry.toString());
            }
            options.add(classpath.toString());
        } else {
            options.add("--class-path");
            options.add("");
        }
        
        return options;
    }

    /**
     * Converts a {@link Diagnostic} from the compiler to a {@link ResultMessage} and calls
     * {@link #addResultMessage(ResultMessage)} with it.
     * 
     * @param diagnostic The {@link Diagnostic} as produced by the compiler.
     * @param submissionDirectory The directory that contains the submission files.
     */
    private void convertDiagnostToResultMessage(Diagnostic<? extends JavaFileObject> diagnostic,
            Path submissionDirectory) {
        
        ResultMessage.MessageType type;
        switch (diagnostic.getKind()) {
        case MANDATORY_WARNING:
        case WARNING:
            type = MessageType.WARNING;
            break;
            
        case ERROR:
            type = MessageType.ERROR;
            break;
            
        default:
            type = null;
            break;
        }
        
        if (type != null) {
            String message = diagnostic.getMessage(Locale.ROOT);
            int linebreak = message.indexOf('\n');
            if (linebreak != -1) {
                message = message.substring(0, linebreak);
            }
            
            ResultMessage resultMessage = new ResultMessage(CHECK_NAME, type, message);
            
            if (diagnostic.getSource() != null) {
                resultMessage.setFile(submissionDirectory.relativize(Path.of(diagnostic.getSource().getName())));
                
                if (diagnostic.getLineNumber() != Diagnostic.NOPOS) {
                    resultMessage.setLine((int) diagnostic.getLineNumber());
                    
                    if (diagnostic.getColumnNumber() != Diagnostic.NOPOS) {
                        resultMessage.setColumn((int) diagnostic.getColumnNumber());
                    }
                }
            }
            
            addResultMessage(resultMessage);
        }
    }
    
}
