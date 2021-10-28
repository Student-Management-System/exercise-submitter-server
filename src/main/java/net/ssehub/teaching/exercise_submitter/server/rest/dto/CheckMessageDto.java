package net.ssehub.teaching.exercise_submitter.server.rest.dto;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.Check;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

/**
 * A message from a {@link Check}.
 * 
 * @author Adam
 */
@Schema(description = "A message created by an automatic check on a submission")
public class CheckMessageDto {

    @Schema(description = "The name of the automatic check", required = true, example = "javac")
    private String checkName;
    
    @Schema(description = "Whether this message is an error or a warning", required = true, example = "ERROR")
    private ResultMessage.MessageType type;
    
    @Schema(description = "The message created by the check", required = true, example = "';' expected")
    private String message;
    
    @Schema(description = "The relative path of the file this message is about", example = "dir/Main.java")
    private String file;
    
    @Schema(description = "The line number in the file that this message is about", example = "4")
    private Integer line;
    
    @Schema(description = "The column of the line in the file that this message is about", example = "43")
    private Integer column;
    
    /**
     * Creates this instance by copying from the given {@link ResultMessage}.
     * 
     * @param message The message to copy from.
     */
    public CheckMessageDto(ResultMessage message) {
        this.checkName = message.getCheckName();
        this.type = message.getType();
        this.message = message.getMessage();
        if (message.getFile() != null) {
            this.file = message.getFile().toString().replace('\\', '/');
        }
        this.line = message.getLine();
        this.column = message.getColumn();
    }
    
    /**
     * Creates a new message with a simple message.
     * <p>
     * Convenience method for test cases.
     *  
     * @param checkName The name of the {@link Check} that created this message.
     * @param type The type of this message.
     * @param message The message describing the result of the check.
     */
    public CheckMessageDto(String checkName, ResultMessage.MessageType type, String message) {
        this.checkName = checkName;
        this.type = type;
        this.message = message;
    }
    
    /**
     * Standard constructor.
     */
    public CheckMessageDto() {
    }

    /**
     * Returns the name of the {@link Check} that created this message.
     * 
     * @return The {@link Check} that created this message.
     */
    public String getCheckName() {
        return checkName;
    }
    
    /**
     * Sets the name of the {@link Check} that created this message.
     * 
     * @param checkName The {@link Check} that created this message.
     */
    public void setCheckName(String checkName) {
        this.checkName = checkName;
    }

    /**
     * Returns the {@link MessageType} of this message.
     * 
     * @return The type of this message.
     */
    public MessageType getType() {
        return type;
    }
    
    /**
     * Sets the {@link MessageType} of this message.
     * 
     * @param type The type of this message.
     */
    public void setType(ResultMessage.MessageType type) {
        this.type = type;
    }
    
    /**
     * Returns the message string of this message.
     * 
     * @return The content of this message.
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Sets the message string of this message.
     * 
     * @param message The content of this message.
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    /**
     * Returns the file location that this message refers to. This is a relative path to the submission.
     * 
     * @return The file location; may be <code>null</code>.
     */
    public String getFile() {
        return file;
    }
    
    /**
     * Sets the file location that this message refers to. This is a relative path to the submission.
     * 
     * @param file The file location; may be <code>null</code>.
     */
    public void setFile(String file) {
        this.file = file;
    }
    
    /**
     * Returns the line location of this message.
     * 
     * @return The line number; may be <code>null</code>.
     */
    public Integer getLine() {
        return line;
    }
    
    /**
     * Sets the line location of this message.
     * 
     * @param line The line number; may be <code>null</code>.
     */
    public void setLine(Integer line) {
        this.line = line;
    }
    
    /**
     * Returns the column location of this message.
     * 
     * @return The column number; may be <code>null</code>.
     */
    public Integer getColumn() {
        return column;
    }
    
    /**
     * Sets the column location of this message.
     * 
     * @param column The column number; may be <code>null</code>.
     */
    public void setColumn(Integer column) {
        this.column = column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkName, column, file, line, message, type);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CheckMessageDto)) {
            return false;
        }
        CheckMessageDto other = (CheckMessageDto) obj;
        return Objects.equals(checkName, other.checkName) && Objects.equals(column, other.column)
                && Objects.equals(file, other.file) && Objects.equals(line, other.line)
                && Objects.equals(message, other.message) && type == other.type;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CheckMessageDto [checkName=");
        builder.append(checkName);
        builder.append(", type=");
        builder.append(type);
        builder.append(", message=");
        builder.append(message);
        builder.append(", file=");
        builder.append(file);
        builder.append(", line=");
        builder.append(line);
        builder.append(", column=");
        builder.append(column);
        builder.append("]");
        return builder.toString();
    }
    
}
