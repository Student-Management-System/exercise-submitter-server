package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.ssehub.teaching.exercise_submitter.server.submission.checks.Check;

/**
 * A configuration for a {@link Check} for an {@link Assignment}, typically specified by the lecturer of the course.
 * 
 * @author Adam
 */
public class CheckConfiguration {

    private String checkName;
    
    private boolean rejecting;
    
    private Map<String, String> properties;
    
    /**
     * Creates a new {@link CheckConfiguration} for the given check.
     * 
     * @param checkName The name of the check.
     * @param rejecting Whether the check should reject a submission on failure.
     */
    public CheckConfiguration(String checkName, boolean rejecting) {
        this.checkName = checkName;
        this.rejecting = rejecting;
        this.properties = new HashMap<>();
    }
    
    /**
     * Sets a check-specific property.
     * 
     * @param key The key of the property.
     * @param value The value of the property.
     */
    public void setProperty(String key, String value) {
        this.properties.put(key, value);
    }
    
    /**
     * Returns the name of the check.
     * 
     * @return The check name.
     */
    public String getCheckName() {
        return checkName;
    }
    
    /**
     * Returns whether the check should reject submissions on failure.
     * 
     * @return Whether the check is rejecting.
     */
    public boolean isRejecting() {
        return rejecting;
    }
    
    /**
     * Returns the check-specific property.
     * 
     * @param key The key of the property.
     * 
     * @return The value of the property, or {@link Optional#empty()} if not set.
     */
    public Optional<String> getProperty(String key) {
        return Optional.ofNullable(properties.get(key));
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkName, properties, rejecting);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CheckConfiguration)) {
            return false;
        }
        CheckConfiguration other = (CheckConfiguration) obj;
        return Objects.equals(checkName, other.checkName) && Objects.equals(properties, other.properties)
                && rejecting == other.rejecting;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("CheckConfiguration [checkName=");
        builder.append(checkName);
        builder.append(", rejecting=");
        builder.append(rejecting);
        builder.append(", properties=");
        builder.append(properties);
        builder.append("]");
        return builder.toString();
    }
    
}
