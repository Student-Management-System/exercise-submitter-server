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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.ssehub.teaching.exercise_submitter.server.checks.FileUtils;

/**
 * Represents the <code>.project</code> configuration file in an eclipse project.
 *  
 * @author Adam
 */
public class EclipseProjectFile {

    public static final String NATURE_JAVA = "org.eclipse.jdt.core.javanature";
    
    public static final String NATURE_CHECKSTYLE = "net.sf.eclipsecs.core.CheckstyleNature";
    
    public static final String BUILDER_JAVA = "org.eclipse.jdt.core.javabuilder";
    
    public static final String BUILDER_CHECKSTYLE = "net.sf.eclipsecs.core.CheckstyleBuilder";
    
    private String name;
    
    private List<String> builders;
    
    private List<String> natures;
    
    /**
     * Parses the given <code>.project</code> file.
     * 
     * @param projectFile The <code>.project</code> file. Note: it is not required that this file is actually called
     *      <code>.project</code>.
     *      
     * @throws InvalidEclipseConfigException If the given file does not contain a valid eclipse
     *      <code>.project</code> file.
     * @throws IOException If reading the specified file fails.
     */
    public EclipseProjectFile(File projectFile) throws InvalidEclipseConfigException, IOException {
        this.builders = new LinkedList<>();
        this.natures = new LinkedList<>();
        
        parseFile(projectFile);
    }
    
    /**
     * Parses the given <code>.project</code> file.
     * 
     * @param projectFile The <code>.project</code> file.
     *      
     * @throws InvalidEclipseConfigException If the given file does not contain a valid eclipse
     *      <code>.project</code> file.
     * @throws IOException If reading the specified file fails.
     */
    private void parseFile(File projectFile) throws InvalidEclipseConfigException, IOException {
        try {
            Document document = FileUtils.parseXml(projectFile);
            
            readSettings(document);
            
        } catch (SAXException e) {
            throw new InvalidEclipseConfigException("Could not parse XML", e);
        }
    }
    
    /**
     * Reads all relevant project configuration settings from the given XMl document.
     * 
     * @param xmlDocument The XML document with the project settings.
     * 
     * @throws InvalidEclipseConfigException If the given XML document does not contain a valid eclipse
     *      <code>.project</code> configuration.
     */
    private void readSettings(Document xmlDocument) throws InvalidEclipseConfigException {
        Node rootNode = xmlDocument.getDocumentElement();
        if (!rootNode.getNodeName().equals("projectDescription")) {
            throw new InvalidEclipseConfigException("Invalid root node: " + rootNode.getNodeName());
        }
        
        Node nameNode = null;
        Node buildSpecNode = null;
        Node naturesNode = null;
        
        NodeList childNodes = rootNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            
            switch (childNode.getNodeName()) {
            case "name":
                if (nameNode != null) {
                    throw new InvalidEclipseConfigException("Found two <name> nodes");
                }
                nameNode = childNode;
                break;
                
            case "buildSpec":
                if (buildSpecNode != null) {
                    throw new InvalidEclipseConfigException("Found two <buildSpec> nodes");
                }
                buildSpecNode = childNode;
                break;
                
            case "natures":
                if (naturesNode != null) {
                    throw new InvalidEclipseConfigException("Found two <natures> nodes");
                }
                naturesNode = childNode;
                break;
            
            default:
                // ignore
                break;
            }
        }
        
        if (nameNode == null) {
            throw new InvalidEclipseConfigException("Didn't find <name> node");
        }
        if (buildSpecNode == null) {
            throw new InvalidEclipseConfigException("Didn't find <buildSpec> node");
        }
        if (naturesNode == null) {
            throw new InvalidEclipseConfigException("Didn't find <natures> node");
        }
        
        readName(nameNode);
        readBuilders(buildSpecNode);
        readNatures(naturesNode);
    }
    
    /**
     * Reads the name of the project.
     * 
     * @param nameNode The name node.
     * 
     * @throws InvalidEclipseConfigException If the name has an invalid format.
     */
    private void readName(Node nameNode) throws InvalidEclipseConfigException {
        assertOnlyTextContent(nameNode);
        this.name = nameNode.getTextContent();
    }
    
    /**
     * Reads the builder names of the project.
     * 
     * @param buildSpecNode The buildSpec node.
     * 
     * @throws InvalidEclipseConfigException If the list of builders has an invalid format.
     */
    private void readBuilders(Node buildSpecNode) throws InvalidEclipseConfigException {
        NodeList childNodes = buildSpecNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node buildCommandNode = childNodes.item(i);
            if (buildCommandNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            
            if (!buildCommandNode.getNodeName().equals("buildCommand")) {
                throw new InvalidEclipseConfigException("Invalid node in <buildSpec>: "
                        + buildCommandNode.getNodeName());
            }
            
            readBuildCommandName(buildCommandNode);
        }
    }
    
    /**
     * Reads the name of a given build command.
     * 
     * @param buildCommandNode The build command node.
     * 
     * @throws InvalidEclipseConfigException If the build command has an invalid format.
     */
    private void readBuildCommandName(Node buildCommandNode) throws InvalidEclipseConfigException {
        boolean foundName = false;
        
        NodeList childNodes = buildCommandNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            
            if (childNode.getNodeName().equals("name")) {
                if (foundName) {
                    throw new InvalidEclipseConfigException("Found multiple <name> nodes in <buildCommand>");
                }
                foundName = true;
                
                assertOnlyTextContent(childNode);
                
                this.builders.add(childNode.getTextContent());
            }
        }
        
        if (!foundName) {
            throw new InvalidEclipseConfigException("Didn't find <name> in <buildCommand>");
        }
    }
    
    /**
     * Reads the natures names of the project.
     * 
     * @param naturesNode The natures node.
     * 
     * @throws If the format of the natures is wrong.
     */
    private void readNatures(Node naturesNode) throws InvalidEclipseConfigException {
        NodeList childNodes = naturesNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node natureNode = childNodes.item(i);
            if (natureNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            
            if (!natureNode.getNodeName().equals("nature")) {
                throw new InvalidEclipseConfigException("Invalid node in <natures>: " + natureNode.getNodeName());
            }
            
            assertOnlyTextContent(natureNode);
            
            this.natures.add(natureNode.getTextContent());
        }
    }

    /**
     * Checks that the given node only has 1 text node as child.
     * 
     * @param node The node to check.
     * 
     * @throws InvalidEclipseConfigException If the node has not only 1 text node.
     */
    private static void assertOnlyTextContent(Node node) throws InvalidEclipseConfigException {
        NodeList nested = node.getChildNodes();
        
        if (nested.getLength() < 1) {
            throw new InvalidEclipseConfigException(
                    "<" + node.getNodeName() + "> should have a nested text node");
        }
        
        if (nested.getLength() > 1) {
            throw new InvalidEclipseConfigException(
                    "<" + node.getNodeName() + "> should only have one nested text node");
        }
        
        if (nested.item(0).getNodeType() != Node.TEXT_NODE) {
            throw new InvalidEclipseConfigException(
                    "<" + node.getNodeName() + "> should only have one nested text node");
        }
    }
    
    /**
     * The name of the eclipse project.
     *  
     * @return The name of the project.
     */
    public String getName() {
        return this.name;
    }
    
    /**
     * The list of builders specified in this configuration.
     * 
     * @return The list of builders as an unmodifiable list.
     */
    public List<String> getBuilders() {
        return Collections.unmodifiableList(this.builders);
    }
    
    /**
     * The list of natures specified in this configuration.
     * 
     * @return The list of builders as an unmodifiable list.
     */
    public List<String> getNatures() {
        return Collections.unmodifiableList(this.natures);
    }
    
}
