package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonParseException;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonValue.ValueType;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.StateEnum;
import net.ssehub.studentmgmt.backend_api.model.ParticipantDto.RoleEnum;

/**
 * An assignment in the student management system.
 * 
 * @author Adam
 */
public class Assignment {

    private String mgmtId;
    
    private String name;
    
    private StateEnum state;
    
    private CollaborationEnum collaboration;
    
    private Map<String, Group> groupsByNames;
    
    private List<CheckConfiguration> checkConfigurations;

    /**
     * Creates an assignment.
     * 
     * @param mgmtId The ID of this assignment in the student management system.
     * @param name The name of this assignment.
     * @param state The state of this assignment.
     * @param collaboration The collaboration type of this assignment.
     */
    Assignment(String mgmtId, String name, StateEnum state, CollaborationEnum collaboration) {
        this.mgmtId = mgmtId;
        this.name = name;
        this.state = state;
        this.collaboration = collaboration;
        this.groupsByNames = new HashMap<>();
        this.checkConfigurations = new LinkedList<>();
    }
    
    /**
     * Returns the ID of this assignment in the student management system.
     * 
     * @return The ID.
     */
    String getMgmtId() {
        return mgmtId;
    }
    
    /**
     * Returns the name of this assignment.
     * 
     * @return The name of this assignment.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the collaboration type of this assignment.
     * 
     * @return The collaboratio type of this assignment.
     */
    public CollaborationEnum getCollaboration() {
        return collaboration;
    }
    
    /**
     * Adds a group to this assignment.
     * 
     * @param group The group to add.
     */
    void addGroup(Group group) {
        this.groupsByNames.put(group.getName(), group); 
    }
    
    /**
     * Returns all groups in this assignment.
     * 
     * @return All groups as an unmodifiable collection.
     */
    public Collection<Group> getGroups() {
        return Collections.unmodifiableCollection(this.groupsByNames.values());
    }
    
    /**
     * Retrieves a group by name.
     * 
     * @param name The name of the group.
     * 
     * @return The group, or {@link Optional#empty()} if a group with this name does not exist.
     */
    public Optional<Group> getGroup(String name) {
        return Optional.ofNullable(groupsByNames.get(name));
    }
    
    /**
     * Returns the state of this assignment.
     * 
     * @return The state of this assignment.
     */
    StateEnum getState() {
        return state;
    }
    
    /**
     * Checks whether the given participant can submit to this assignment. This is based on the state of this assignment
     * and the role of the participant. 
     * 
     * @param participant The participant that should be checked.
     * 
     * @return Whether the given participant can submit to this assignment.
     */
    public boolean canSubmit(Participant participant) {
        boolean isTutor = participant.getRole() == RoleEnum.LECTURER || participant.getRole() == RoleEnum.TUTOR;
        boolean isStudent = participant.getRole() == RoleEnum.STUDENT;
        
        return isTutor || (this.state == StateEnum.IN_PROGRESS && isStudent);
    }
    
    /**
     * Checks whether the given participant can retrieve previous submission to this assignment. This is based on the
     * state of this assignment and the role of the participant. 
     * 
     * @param participant The participant that should be checked.
     * 
     * @return Whether the given participant can retrieve previous submissions to this assignment.
     */
    public boolean canReplay(Participant participant) {
        boolean isTutor = participant.getRole() == RoleEnum.LECTURER || participant.getRole() == RoleEnum.TUTOR;
        boolean isStudent = participant.getRole() == RoleEnum.STUDENT;
        
        boolean stateInProgressOrEvaluated = this.state == StateEnum.IN_PROGRESS || this.state == StateEnum.EVALUATED;
        
        return isTutor || (stateInProgressOrEvaluated && isStudent);
    }
    
    /**
     * Sets the {@link CheckConfiguration}s for this assignment.
     * 
     * @param checkConfigJson A JSON string representing the configuration.
     */
    public void setCheckConfigurationString(String checkConfigJson) {
        this.checkConfigurations.clear();
        
        try {
            JsonArray array = Json.createReader(new StringReader(checkConfigJson)).readArray();
            for (int i = 0; i < array.size(); i++) {
                JsonObject object = array.getJsonObject(i);
                if (!object.containsKey("check")) {
                    throw new JsonException("Missing key \"check\"");
                }
                JsonString checkName = object.getJsonString("check");
                JsonValue rejecting = object.getOrDefault("rejecting", JsonValue.FALSE);
                if (rejecting.getValueType() != ValueType.TRUE && rejecting.getValueType() != ValueType.FALSE) {
                    throw new JsonException("\"rejecting\" must be a boolean");
                }
                CheckConfiguration check = new CheckConfiguration(checkName.getString(), rejecting == JsonValue.TRUE);
                
                for (String key : object.keySet()) {
                    if (!key.equals("check") && !key.equals("rejecting")) {
                        check.setProperty(key, object.getJsonString(key).getString());
                    }
                }
                
                this.checkConfigurations.add(check);
            }
            
        } catch (JsonException | JsonParseException | ClassCastException e) {
            throw new IllegalArgumentException("Failed to parse check configuration for assignment " + name
                    + ": " + checkConfigJson, e);
        }
    }

    /**
     * Returns the {@link CheckConfiguration} set for this assignment.
     * 
     * @return The {@link CheckConfiguration} for this assignment.
     */
    public List<CheckConfiguration> getCheckConfigurations() {
        return Collections.unmodifiableList(this.checkConfigurations);
    }
    
}
