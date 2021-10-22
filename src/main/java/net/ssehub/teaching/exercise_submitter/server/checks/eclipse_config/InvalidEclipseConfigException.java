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

/**
 * An exception signaling that an eclipse configuration file has an invalid format.
 * 
 * @author Adam
 */
public class InvalidEclipseConfigException extends Exception {

    private static final long serialVersionUID = -3078308683688046114L;

    /**
     * Creates an {@link InvalidEclipseConfigException}.
     * 
     * @param message A message explaining the exception.
     * @param cause Another exception that caused this exception.
     */
    public InvalidEclipseConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an {@link InvalidEclipseConfigException}.
     * 
     * @param message A message explaining the exception.
     */
    public InvalidEclipseConfigException(String message) {
        super(message);
    }

}
