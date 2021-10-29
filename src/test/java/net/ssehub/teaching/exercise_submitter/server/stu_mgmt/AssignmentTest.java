package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.StateEnum;
import net.ssehub.studentmgmt.backend_api.model.ParticipantDto.RoleEnum;

public class AssignmentTest {

    private static Map<RoleEnum, Integer> ROLE_INDEX = Map.of(
            RoleEnum.LECTURER, 0,
            RoleEnum.TUTOR, 1,
            RoleEnum.STUDENT, 2);
    
    private static Map<StateEnum, Integer> STATE_INDEX = Map.of(
            StateEnum.INVISIBLE, 0,
            StateEnum.CLOSED, 1,
            StateEnum.IN_PROGRESS, 2,
            StateEnum.IN_REVIEW, 3,
            StateEnum.EVALUATED, 4);
    
    private static boolean[][] SUBMISSIONS = {
                     /* INVISIBLE */ /* CLOSED */ /* IN_PROGRESS */ /* IN_REVIEW */ /* EVALUATED */
        /*LECTURER*/ {  true,           true,        true,             true,           true},
        /*TUTOR*/    {  true,           true,        true,             true,           true},
        /*STUDENT*/  {  false,          false,       true,             false,          false}
    };
    
    private static boolean[][] REPLAY = {
                         /* INVISIBLE */ /* CLOSED */ /* IN_PROGRESS */ /* IN_REVIEW */ /* EVALUATED */
            /*LECTURER*/ {  true,           true,        true,             true,           true},
            /*TUTOR*/    {  true,           true,        true,             true,           true},
            /*STUDENT*/  {  false,          false,       true,             false,          true}
    };
    
    @Test
    public void canSubmit() {
        for (RoleEnum role : RoleEnum.values()) {
            Participant participant = new Participant("abc", "participant", role);
            for (StateEnum  state : StateEnum.values()) {
                Assignment assignment = new Assignment("123", "Homework01", state, CollaborationEnum.SINGLE);
                
                boolean expected = SUBMISSIONS[ROLE_INDEX.get(role)][STATE_INDEX.get(state)];
                assertEquals(
                        expected,
                        assignment.canSubmit(participant),
                        "role " + role + " trying to submit to " + state + " should return " + expected);
            }
        }
    }
    
    @Test
    public void canReplay() {
        for (RoleEnum role : RoleEnum.values()) {
            Participant participant = new Participant("abc", "participant", role);
            for (StateEnum  state : StateEnum.values()) {
                Assignment assignment = new Assignment("123", "Homework01", state, CollaborationEnum.SINGLE);
                
                boolean expected = REPLAY[ROLE_INDEX.get(role)][STATE_INDEX.get(state)];
                assertEquals(
                        expected,
                        assignment.canReplay(participant),
                        "role " + role + " trying to replay " + state + " should return " + expected);
            }
        }
    }
    
    @Nested
    public class SetCheckConfigurationString {
        
        @Test
        public void notAnArrayThrows() {
            Assignment a = new Assignment("", "", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
            assertThrows(IllegalArgumentException.class, () -> a.setCheckConfigurationString("{}"));
        }
        
        @Test
        public void emptyArray() {
            Assignment a = new Assignment("", "", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
            assertDoesNotThrow(() -> a.setCheckConfigurationString("[]"));
            assertEquals(Collections.emptyList(), a.getCheckConfigurations());
        }
        
        @Test
        public void arrayElementNotObjectThrows() {
            Assignment a = new Assignment("", "", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
            assertThrows(IllegalArgumentException.class, () -> a.setCheckConfigurationString("[\"str\"]"));
        }
        
        @Test
        public void missingCheckNameThrows() {
            Assignment a = new Assignment("", "", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
            assertThrows(IllegalArgumentException.class, () -> a.setCheckConfigurationString("[{}]"));
        }
        
        @Test
        public void checkNameNotStringThrows() {
            Assignment a = new Assignment("", "", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
            assertThrows(IllegalArgumentException.class, () -> a.setCheckConfigurationString("[{\"check\":3}]"));
        }
        
        @Test
        public void rejectingNotBooleanThrows() {
            Assignment a = new Assignment("", "", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
            assertThrows(IllegalArgumentException.class, () -> a.setCheckConfigurationString("[{\"check\":\"\",\"rejecting\":3}]"));
        }
        
        @Test
        public void onlyCheckNameDefaultForRejectinganProperties() {
            Assignment a = new Assignment("", "", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
            assertDoesNotThrow(() -> a.setCheckConfigurationString("[{\"check\":\"javac\"}]"));
            assertEquals(Arrays.asList(new CheckConfiguration("javac", false)), a.getCheckConfigurations());
        }
        
        @Test
        public void rejectingTrue() {
            Assignment a = new Assignment("", "", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
            assertDoesNotThrow(() -> a.setCheckConfigurationString("[{\"check\":\"checkstyle\",\"rejecting\":true}]"));
            assertEquals(Arrays.asList(new CheckConfiguration("checkstyle", true)), a.getCheckConfigurations());
        }
        
        @Test
        public void propertyValueNotStringThrows() {
            Assignment a = new Assignment("", "", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
            assertThrows(IllegalArgumentException.class, () -> a.setCheckConfigurationString("[{\"check\":\"javac\",\"a\":1}]"));
        }
        
        @Test
        public void propertiesSet() {
            Assignment a = new Assignment("", "", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
            assertDoesNotThrow(() -> a.setCheckConfigurationString("[{\"check\":\"javac\",\"a\":\"b\",\"c\":\"d\"}]"));
            
            CheckConfiguration expected = new CheckConfiguration("javac", false);
            expected.setProperty("a", "b");
            expected.setProperty("c", "d");
            assertEquals(Arrays.asList(expected), a.getCheckConfigurations());
        }
        
        @Test
        public void multipleChecks() {
            Assignment a = new Assignment("", "", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
            assertDoesNotThrow(() -> a.setCheckConfigurationString("[{\"check\":\"javac\"},{\"check\":\"checkstyle\"}]"));
            assertEquals(Arrays.asList(new CheckConfiguration("javac", false), new CheckConfiguration("checkstyle", false)),
                    a.getCheckConfigurations());
        }
        
    }
    
}
