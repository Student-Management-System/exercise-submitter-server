package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

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
            Participant participant = new Participant("participant", role);
            for (StateEnum  state : StateEnum.values()) {
                Assignment assignment = new Assignment("Homework01", state, CollaborationEnum.SINGLE);
                
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
            Participant participant = new Participant("participant", role);
            for (StateEnum  state : StateEnum.values()) {
                Assignment assignment = new Assignment("Homework01", state, CollaborationEnum.SINGLE);
                
                boolean expected = REPLAY[ROLE_INDEX.get(role)][STATE_INDEX.get(state)];
                assertEquals(
                        expected,
                        assignment.canReplay(participant),
                        "role " + role + " trying to replay " + state + " should return " + expected);
            }
        }
    }
    
}
