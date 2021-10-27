package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.backend_api.ApiClient;
import net.ssehub.studentmgmt.backend_api.ApiException;
import net.ssehub.studentmgmt.backend_api.api.AssessmentApi;
import net.ssehub.studentmgmt.backend_api.model.AssessmentCreateDto;
import net.ssehub.studentmgmt.backend_api.model.AssessmentDto;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.StateEnum;
import net.ssehub.studentmgmt.backend_api.model.MarkerDto.SeverityEnum;
import net.ssehub.studentmgmt.backend_api.model.PartialAssessmentDto;
import net.ssehub.studentmgmt.backend_api.model.ParticipantDto.RoleEnum;
import net.ssehub.studentmgmt.docker.StuMgmtDocker;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.AssignmentState;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.Collaboration;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

public class StuMgmtViewIT {

    private static StuMgmtDocker docker;
    
    private static String homework01Id;
    
    private static String testatId;
    
    private static String theOddsId;
    
    private static String theEvensId;
    
    private static String student1Id;
    
    private static String student2Id;
    
    private static String student3Id;
    
    @BeforeAll
    public static void startServer() {
        docker = new StuMgmtDocker();
        
        docker.createUser("teacher", "abcdefgh");
        student1Id = docker.createUser("student1", "abcdefgh");
        student2Id = docker.createUser("student2", "abcdefgh");
        student3Id = docker.createUser("student3", "abcdefgh");
        
        docker.createCourse("bar", "sose20", "Barfoo", "teacher");
        
        String course = docker.createCourse("foo", "wise2122", "Foobar", "teacher");
        docker.enrollStudent(course, "student1");
        docker.enrollStudent(course, "student2");
        docker.enrollStudent(course, "student3");
        
        theOddsId = docker.createGroup(course, "TheOdds", "student1", "student3");
        theEvensId = docker.createGroup(course, "TheEvens", "student2");
        
        homework01Id = docker.createAssignment(course, "Homework01", AssignmentState.SUBMISSION, Collaboration.GROUP);
        testatId = docker.createAssignment(course, "Testat", AssignmentState.INVISIBLE, Collaboration.SINGLE);
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
    
    @Nested
    public class SendSubmissionResult {
        
        @Test
        public void draftAssesmentCreateForGroupAssignment() throws ApiException {
            StuMgmtView view = assertDoesNotThrow(() -> new StuMgmtView(docker.getStuMgmtUrl(), docker.getAuthUrl(),
                    "teacher", "abcdefgh"));
            assertDoesNotThrow(() -> view.fullReload());
            
            ResultMessage m2 = new ResultMessage("check2", MessageType.ERROR, "this is bad!");
            m2.setFile(new File("dir/Main.java"));
            m2.setLine(5);
            m2.setColumn(8);
            view.sendSubmissionResult(new SubmissionTarget("foo-wise2122", "Homework01", "TheOdds"),
                    Arrays.asList(new ResultMessage("check1", MessageType.WARNING, "some warning message"),
                            m2));
            
            ApiClient client = new ApiClient();
            client.setBasePath(docker.getStuMgmtUrl());
            client.setAccessToken(docker.getAuthToken("teacher"));
            
            AssessmentApi api = new AssessmentApi(client);
            
            List<AssessmentDto> assessments = api.getAssessmentsForAssignment(
                    "foo-wise2122", homework01Id, null, null, null, theOddsId, null, null, null);
            
            assertAll(
                () -> assertEquals(1, assessments.size()),
                () -> assertEquals("teacher", assessments.get(0).getCreator().getUsername()),
                () -> assertEquals(true, assessments.get(0).isIsDraft()),
                () -> assertEquals("TheOdds", assessments.get(0).getGroup().getName()),
                () -> assertNull(assessments.get(0).getUserId()),

                () -> assertEquals(1, assessments.get(0).getPartialAssessments().size()),
                () -> assertEquals("Automatic Checks", assessments.get(0).getPartialAssessments().get(0).getTitle()),
                () -> assertEquals("exercise-submitter-checks", assessments.get(0).getPartialAssessments().get(0).getKey()),
                () -> assertEquals(2, assessments.get(0).getPartialAssessments().get(0).getMarkers().size()),
                
                () -> assertEquals("(check1) some warning message", assessments.get(0).getPartialAssessments().get(0).getMarkers().get(0).getComment()),
                () -> assertEquals(SeverityEnum.WARNING, assessments.get(0).getPartialAssessments().get(0).getMarkers().get(0).getSeverity()),
                
                () -> assertEquals("(check2) this is bad!", assessments.get(0).getPartialAssessments().get(0).getMarkers().get(1).getComment()),
                () -> assertEquals(SeverityEnum.ERROR, assessments.get(0).getPartialAssessments().get(0).getMarkers().get(1).getSeverity()),
                () -> assertEquals("dir/Main.java", assessments.get(0).getPartialAssessments().get(0).getMarkers().get(1).getPath()),
                () -> assertEquals(5, assessments.get(0).getPartialAssessments().get(0).getMarkers().get(1).getStartLineNumber().intValue()),
                () -> assertEquals(5, assessments.get(0).getPartialAssessments().get(0).getMarkers().get(1).getEndLineNumber().intValue()),
                () -> assertEquals(8, assessments.get(0).getPartialAssessments().get(0).getMarkers().get(1).getStartColumn().intValue()),
                () -> assertEquals(8, assessments.get(0).getPartialAssessments().get(0).getMarkers().get(1).getEndColumn().intValue())
            );
        }
        
        @Test
        public void draftAssesmentCreateForSingleAssignment() throws ApiException {
            StuMgmtView view = assertDoesNotThrow(() -> new StuMgmtView(docker.getStuMgmtUrl(), docker.getAuthUrl(),
                    "teacher", "abcdefgh"));
            assertDoesNotThrow(() -> view.fullReload());
            
            view.sendSubmissionResult(new SubmissionTarget("foo-wise2122", "Testat", "student1"),
                    Arrays.asList(new ResultMessage("check1", MessageType.WARNING, "some warning message"),
                            new ResultMessage("check2", MessageType.ERROR, "this is bad!")));
            
            ApiClient client = new ApiClient();
            client.setBasePath(docker.getStuMgmtUrl());
            client.setAccessToken(docker.getAuthToken("teacher"));
            
            AssessmentApi api = new AssessmentApi(client);
            
            List<AssessmentDto> assessments = api.getAssessmentsForAssignment(
                    "foo-wise2122", testatId, null, null, null, null, student1Id, null, null);
            
            assertAll(
                () -> assertEquals(1, assessments.size()),
                () -> assertEquals("teacher", assessments.get(0).getCreator().getUsername()),
                () -> assertEquals(true, assessments.get(0).isIsDraft()),
                () -> assertNull(assessments.get(0).getGroupId()),
                () -> assertEquals("student1", assessments.get(0).getParticipant().getUsername()),
                
                () -> assertEquals(1, assessments.get(0).getPartialAssessments().size()),
                () -> assertEquals("Automatic Checks", assessments.get(0).getPartialAssessments().get(0).getTitle()),
                () -> assertEquals("exercise-submitter-checks", assessments.get(0).getPartialAssessments().get(0).getKey()),
                () -> assertEquals(2, assessments.get(0).getPartialAssessments().get(0).getMarkers().size()),
                
                () -> assertEquals("(check1) some warning message", assessments.get(0).getPartialAssessments().get(0).getMarkers().get(0).getComment()),
                () -> assertEquals(SeverityEnum.WARNING, assessments.get(0).getPartialAssessments().get(0).getMarkers().get(0).getSeverity()),
                
                () -> assertEquals("(check2) this is bad!", assessments.get(0).getPartialAssessments().get(0).getMarkers().get(1).getComment()),
                () -> assertEquals(SeverityEnum.ERROR, assessments.get(0).getPartialAssessments().get(0).getMarkers().get(1).getSeverity())
            );
        }
        
        @Test
        public void assessmentUpdatedOnSecondSubmission() throws ApiException {
            StuMgmtView view = assertDoesNotThrow(() -> new StuMgmtView(docker.getStuMgmtUrl(), docker.getAuthUrl(),
                    "teacher", "abcdefgh"));
            assertDoesNotThrow(() -> view.fullReload());
            
            view.sendSubmissionResult(new SubmissionTarget("foo-wise2122", "Homework01", "TheEvens"),
                    Arrays.asList(new ResultMessage("check1", MessageType.WARNING, "old")));
            
            view.sendSubmissionResult(new SubmissionTarget("foo-wise2122", "Homework01", "TheEvens"),
                    Arrays.asList(new ResultMessage("check1", MessageType.WARNING, "new")));
            
            ApiClient client = new ApiClient();
            client.setBasePath(docker.getStuMgmtUrl());
            client.setAccessToken(docker.getAuthToken("teacher"));
            
            AssessmentApi api = new AssessmentApi(client);
            
            List<AssessmentDto> assessments = api.getAssessmentsForAssignment(
                    "foo-wise2122", homework01Id, null, null, null, theEvensId, null, null, null);
            
            assertAll(
                () -> assertEquals(1, assessments.size()),
                () -> assertEquals("teacher", assessments.get(0).getCreator().getUsername()),
                () -> assertEquals(true, assessments.get(0).isIsDraft()),
                () -> assertEquals("TheEvens", assessments.get(0).getGroup().getName()),
                
                () -> assertEquals(1, assessments.get(0).getPartialAssessments().size()),
                () -> assertEquals("(check1) new", assessments.get(0).getPartialAssessments().get(0).getMarkers().get(0).getComment())
            );
        }
        
        @Test
        public void updatingAssessmentPreservesOtherPartialAssessments() throws ApiException {
            StuMgmtView view = assertDoesNotThrow(() -> new StuMgmtView(docker.getStuMgmtUrl(), docker.getAuthUrl(),
                    "teacher", "abcdefgh"));
            assertDoesNotThrow(() -> view.fullReload());
            
            ApiClient client = new ApiClient();
            client.setBasePath(docker.getStuMgmtUrl());
            client.setAccessToken(docker.getAuthToken("teacher"));
            AssessmentApi api = new AssessmentApi(client);
            
            AssessmentCreateDto preexisting = new AssessmentCreateDto();
            preexisting.setIsDraft(true);
            preexisting.setAssignmentId(testatId);
            preexisting.setUserId(student2Id);
            preexisting.addPartialAssessmentsItem(new PartialAssessmentDto().title("Preexisting").key("pre-existing"));
            
            api.createAssessment(preexisting, "foo-wise2122", testatId);
            
            view.sendSubmissionResult(new SubmissionTarget("foo-wise2122", "Testat", "student2"),
                    Arrays.asList(new ResultMessage("check1", MessageType.WARNING, "message")));
            
            List<AssessmentDto> assessments = api.getAssessmentsForAssignment(
                    "foo-wise2122", testatId, null, null, null, null, student2Id, null, null);
            
            assertAll(
                () -> assertEquals(1, assessments.size()),
                () -> assertEquals("teacher", assessments.get(0).getCreator().getUsername()),
                () -> assertEquals(true, assessments.get(0).isIsDraft()),
                () -> assertNull(assessments.get(0).getGroupId()),
                () -> assertEquals("student2", assessments.get(0).getParticipant().getUsername()),
                
                () -> assertEquals(2, assessments.get(0).getPartialAssessments().size()),
                () -> assertEquals("Preexisting", assessments.get(0).getPartialAssessments().get(0).getTitle()),
                () -> assertEquals("(check1) message", assessments.get(0).getPartialAssessments().get(1).getMarkers().get(0).getComment())
            );
        }
        
        @Test
        public void assessmentNotUpdatedIfNotDraft() throws ApiException {
            StuMgmtView view = assertDoesNotThrow(() -> new StuMgmtView(docker.getStuMgmtUrl(), docker.getAuthUrl(),
                    "teacher", "abcdefgh"));
            assertDoesNotThrow(() -> view.fullReload());
            
            ApiClient client = new ApiClient();
            client.setBasePath(docker.getStuMgmtUrl());
            client.setAccessToken(docker.getAuthToken("teacher"));
            AssessmentApi api = new AssessmentApi(client);
            
            AssessmentCreateDto preexisting = new AssessmentCreateDto();
            preexisting.setIsDraft(false);
            preexisting.setAssignmentId(testatId);
            preexisting.setUserId(student3Id);
            
            api.createAssessment(preexisting, "foo-wise2122", testatId);
            
            view.sendSubmissionResult(new SubmissionTarget("foo-wise2122", "Testat", "student3"),
                    Arrays.asList(new ResultMessage("check1", MessageType.WARNING, "message")));
            
            List<AssessmentDto> assessments = api.getAssessmentsForAssignment(
                    "foo-wise2122", testatId, null, null, null, null, student3Id, null, null);
            
            assertAll(
                () -> assertEquals(1, assessments.size()),
                () -> assertEquals("teacher", assessments.get(0).getCreator().getUsername()),
                () -> assertEquals(false, assessments.get(0).isIsDraft()),
                () -> assertNull(assessments.get(0).getGroupId()),
                () -> assertEquals("student3", assessments.get(0).getParticipant().getUsername()),
                
                () -> assertEquals(0, assessments.get(0).getPartialAssessments().size())
            );
        }
        
    }
    
}
