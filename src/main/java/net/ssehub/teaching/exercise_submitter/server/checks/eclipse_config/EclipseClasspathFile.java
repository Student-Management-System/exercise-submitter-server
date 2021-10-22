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
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.ssehub.teaching.exercise_submitter.server.checks.FileUtils;
import net.ssehub.teaching.exercise_submitter.server.checks.eclipse_config.ClasspathEntry.Kind;

/**
 * Represents the <code>.classpath</code> configuration file in an eclipse project.
 * 
 * @author Adam
 */
public class EclipseClasspathFile {
    
    private List<ClasspathEntry> entries;

    /**
     * Parses the given <code>.classpath</code> file.
     * 
     * @param classpathFile The <code>.classpath</code> file. Note: it is not required that this file is actually called
     *      <code>.classpath</code>.
     *      
     * @throws InvalidEclipseConfigException If the given file does not contain a valid eclipse
     *      <code>.classpath</code> file.
     * @throws IOException If reading the specified file fails.
     */
    public EclipseClasspathFile(File classpathFile) throws InvalidEclipseConfigException, IOException {
        this.entries = new LinkedList<>();
        parseFile(classpathFile);
    }
    
    /**
     * Parses the given file.
     * 
     * @param classpathFile The file to parse.
     *      
     * @throws InvalidEclipseConfigException If the given file does not contain a valid eclipse
     *      <code>.classpath</code> file.
     * @throws IOException If reading the specified file fails.
     */
    private void parseFile(File classpathFile) throws InvalidEclipseConfigException, IOException {
        try {
            Document document = FileUtils.parseXml(classpathFile);
            readClasspathEntries(document);
        } catch (SAXException e) {
            throw new InvalidEclipseConfigException("Could not parse XML", e);
        }
    }
    
    /**
     * Reads all classpath entries from the given XML document.
     * 
     * @param xmlDocument The XML document to read the entries from.
     * 
     * @throws InvalidEclipseConfigException If the XML structure is not valid for eclipse
     *      <code>.classpath</code> files.
     */
    private void readClasspathEntries(Document xmlDocument) throws InvalidEclipseConfigException {
        Node rootNode = xmlDocument.getDocumentElement();
        if (!rootNode.getNodeName().equals("classpath")) {
            throw new InvalidEclipseConfigException("Invalid root node: " + rootNode.getNodeName());
        }
        
        NodeList classpathEntries = rootNode.getChildNodes();
        for (int i = 0; i < classpathEntries.getLength(); i++) {
            Node classpathEntry = classpathEntries.item(i);
            
            if (classpathEntry.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            
            if (!classpathEntry.getNodeName().equals("classpathentry")) {
                throw new InvalidEclipseConfigException("Invalid node in <classpath>: " + classpathEntry.getNodeName());
            }
            
            NamedNodeMap attributes = classpathEntry.getAttributes();
            Node kindAttribute = attributes.getNamedItem("kind");
            Node pathAttribute = attributes.getNamedItem("path");
            
            if (kindAttribute == null) {
                throw new InvalidEclipseConfigException("Entry does not have a \"kind\" attribute");
            }
            if (pathAttribute == null) {
                throw new InvalidEclipseConfigException("Entry does not have a \"path\" attribute");
            }
            
            Kind kind;
            switch (kindAttribute.getTextContent()) {
            case "src":
                kind = Kind.SOURCE;
                break;
                
            case "con":
                kind = Kind.CONTAINER;
                break;
                
            case "output":
                kind = Kind.OUTPUT;
                break;
                
            case "lib":
                kind = Kind.LIBRARY;
                break;
                
            default:
                kind = Kind.OTHER;
                break;
            }
            
            this.entries.add(new ClasspathEntry(kind, new File(pathAttribute.getTextContent())));
        }
    }
    
    /**
     * Returns all {@link ClasspathEntry}s in this configuration file.
     *  
     * @return An unmodifiable list of all {@link ClasspathEntry}s as read from the configuration file.
     */
    public List<ClasspathEntry> getClasspathEntries() {
        return Collections.unmodifiableList(this.entries);
    }
    
}
