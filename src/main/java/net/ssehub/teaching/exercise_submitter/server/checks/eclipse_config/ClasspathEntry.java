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
package net.ssehub.teaching.exercise_submitter.server.checks.eclipse_config;

import java.io.File;
import java.util.Objects;

/**
 * An entry in the <code>.classpath</code> configuration file in an eclipse project. 
 */
public class ClasspathEntry {

    /**
     * The kind of a classpath entry.
     */
    public enum Kind {
        SOURCE,
        CONTAINER,
        LIBRARY,
        OUTPUT,
        OTHER,
    }
    
    private Kind kind;
    
    private File path;

    /**
     * Creates a {@link ClasspathEntry}.
     * 
     * @param kind The {@link Kind} of this entry.
     * @param path The path of this entry relative to the eclipse project root.
     */
    public ClasspathEntry(Kind kind, File path) {
        this.kind = kind;
        this.path = path;
    }
    
    /**
     * Returns the kind of entry this is.
     * 
     * @return The {@link Kind} of this entry.
     */
    public Kind getKind() {
        return kind;
    }
    
    /**
     * Returns the path of this entry relative to the eclipse project root.
     * 
     * @return The path of this entry.
     */
    public File getPath() {
        return path;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(kind, path);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ClasspathEntry)) {
            return false;
        }
        ClasspathEntry other = (ClasspathEntry) obj;
        return kind == other.kind && Objects.equals(path, other.path);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ClasspathEntry [kind=");
        builder.append(kind);
        builder.append(", path=");
        builder.append(path);
        builder.append("]");
        return builder.toString();
    }
    
}
