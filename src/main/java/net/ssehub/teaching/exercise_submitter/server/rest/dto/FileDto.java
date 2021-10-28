package net.ssehub.teaching.exercise_submitter.server.rest.dto;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A file in a submission.
 * 
 * @author Adam
 */
@Schema(description = "A single file in a submission")
public class FileDto {

    @Schema(
        description = "Relative path of this file in the submission directory",
        required = true,
        example = "dir/Main.java"
    )
    private String path;
    
    @Schema(
        description = "Base64-encoded content of the file",
        required = true,
        example = "cGFja2FnZSBkaXI7CgpwdWJsaWMgY2xhc3MgTWFpbiB7CgogICAgcHVibGljIHN0YXRpYyB2b2lk"
                + "IG1haW4oU3RyaW5nW10gYXJncykgewogICAgICAgIFN5c3RlbS5vdXQucHJpbnRsbigiSGVsbG8g"
                + "V29ybGQhIik7CiAgICB9Cgp9Cg==")
    private String content;

    /**
     * Standard constructor for JSON de-serializing.
     */
    public FileDto() {
    }
    
    /**
     * Creates a {@link FileDto} with given path and content.
     * 
     * @param path The relative path of this file in the submission directory.
     * @param content The content of the file.
     */
    public FileDto(String path, byte[] content) {
        this.path = path;
        this.content = Base64.getEncoder().encodeToString(content);
    }
    
    /**
     * Convenience method to create an UTF-8 file from a string.
     * 
     * @param path The relative path of this file in the submission directory.
     * @param nonEncodedcontent The string content of the file.
     */
    public FileDto(String path, String nonEncodedcontent) {
        this(path, nonEncodedcontent.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Returns the relative path of this file in the submission directory.
     * 
     * @return The relative file path.
     */
    public String getPath() {
        return path;
    }

    /**
     * Sets the relative path of this file in the submission directory.
     * 
     * @param path The relative file path.
     */
    public void setPath(String path) {
        this.path = path;
    }
    
    /**
     * Gets the base64-encoded of this file.
     * 
     * @return The base64-encoded content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the base64-encoded of this file.
     * 
     * @param content The base64-encoded content.
     */
    public void setContent(String content) {
        this.content = content;
    }
    
}
