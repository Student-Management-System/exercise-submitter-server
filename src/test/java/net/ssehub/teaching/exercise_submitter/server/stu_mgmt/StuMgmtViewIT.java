package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.StateEnum;
import net.ssehub.studentmgmt.backend_api.model.ParticipantDto.RoleEnum;
import net.ssehub.studentmgmt.docker.StuMgmtDocker;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.AssignmentState;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.Collaboration;

public class StuMgmtViewIT {

    private static StuMgmtDocker docker;
    
    @BeforeAll
    public static void startServer() {
        docker = new StuMgmtDocker();
        
        docker.createUser("teacher", "abcdefgh");
        docker.createUser("student1", "abcdefgh");
        docker.createUser("student2", "abcdefgh");
        docker.createUser("student3", "abcdefgh");
        
        docker.createCourse("bar", "sose20", "Barfoo", "teacher");
        
        String course = docker.createCourse("foo", "wise2122", "Foobar", "teacher");
        docker.enrollStudent(course, "student1");
        docker.enrollStudent(course, "student2");
        docker.enrollStudent(course, "student3");
        
        docker.createGroup(course, "TheOdds", "student1", "student3");
        docker.createGroup(course, "TheEvens", "student2");
        
        docker.createAssignment(course, "Homework01", AssignmentState.SUBMISSION, Collaboration.GROUP);
        docker.createAssignment(course, "Testat", AssignmentState.INVISIBLE, Collaboration.SINGLE);
    }
    
    @AfterAll
    public static void stopServer() {
        docker.close();
    }
    
    @Test
    public void fullReload() {
        StuMgmtView view = assertDoesNotThrow(() -> new StuMgmtView(docker.getStuMgmtUrl(), docker.getAuthUrl(),
                "teacher", "abcdefgh"));
        
        assertDoesNotThrow(() -> view.fullReload());
        
        assertEquals(2, view.getCourses().size());
        
        // bar
        
        Course bar = view.getCourse("bar-sose20").orElseThrow();
        assertEquals("bar-sose20", bar.getId());
        assertEquals(1, bar.getParticipants().size());
        assertTrue(bar.getParticipant("teacher").isPresent());
        
        // foo
        
        Course foo = view.getCourse("foo-wise2122").orElseThrow();
        assertEquals("foo-wise2122", foo.getId());
        
        assertEquals(4, foo.getParticipants().size());
        Participant teacher = foo.getParticipant("teacher").get();
        Participant student1 = foo.getParticipant("student1").get();
        Participant student2 = foo.getParticipant("student2").get();
        Participant student3 = foo.getParticipant("student3").get();
        
        assertEquals(RoleEnum.LECTURER, teacher.getRole());
        assertEquals(RoleEnum.STUDENT, student1.getRole());
        assertEquals(RoleEnum.STUDENT, student2.getRole());
        assertEquals(RoleEnum.STUDENT, student3.getRole());
        
        assertEquals("teacher", teacher.getName());
        assertEquals("student1", student1.getName());
        assertEquals("student2", student2.getName());
        assertEquals("student3", student3.getName());
        
        
        assertEquals(2, foo.getAssignments().size());
        Assignment homework01 = foo.getAssignment("Homework01").get();
        Assignment testat = foo.getAssignment("Testat").get();
        
        assertEquals("Homework01", homework01.getName());
        assertEquals(StateEnum.IN_PROGRESS, homework01.getState());
        assertEquals(CollaborationEnum.GROUP, homework01.getCollaboration());
        assertEquals(2, homework01.getGroups().size());
        
        assertEquals("Testat", testat.getName());
        assertEquals(StateEnum.INVISIBLE, testat.getState());
        assertEquals(CollaborationEnum.SINGLE, testat.getCollaboration());
        assertEquals(0, testat.getGroups().size());
        
        
        Group odds = homework01.getGroup("TheOdds").get();
        Group evens = homework01.getGroup("TheEvens").get();
        
        assertEquals("TheOdds", odds.getName());
        assertEquals("TheEvens", evens.getName());
        
        assertTrue(odds.hasParticipant(student1));
        assertTrue(odds.hasParticipant(student3));
        assertFalse(odds.hasParticipant(teacher));
        assertFalse(odds.hasParticipant(student2));
        
        assertTrue(evens.hasParticipant(student2));
        assertFalse(evens.hasParticipant(teacher));
        assertFalse(evens.hasParticipant(student1));
        assertFalse(evens.hasParticipant(student3));
    }
    
}
