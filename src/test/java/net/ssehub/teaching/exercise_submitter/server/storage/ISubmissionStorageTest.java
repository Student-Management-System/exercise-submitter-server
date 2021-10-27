package net.ssehub.teaching.exercise_submitter.server.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.StateEnum;
import net.ssehub.studentmgmt.backend_api.model.ParticipantDto.RoleEnum;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Assignment;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Course;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.EmptyStuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtLoadingException;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;

public class ISubmissionStorageTest {

    @Test
    public void createOrUpdateAssignmentsFromViewSingleAssignment() throws StuMgmtLoadingException {
        StuMgmtView view = new EmptyStuMgmtView() {
            @Override
            public void fullReload() {
                Course course = createCourse("foo-123");
                createAssignment(course, "123", "Test01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                createAssignment(course, "312", "Test02", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                createParticipant(course, "def", "student2", RoleEnum.STUDENT);
                createParticipant(course, "ghi", "teacher", RoleEnum.LECTURER);
            }
        };
        view.fullReload();
        
        Set<String> addedFolders = new HashSet<>();
        ISubmissionStorage storage = new EmptyStorage() {
            @Override
            public void createOrUpdateAssignment(String course, String assignmentName, String... newGroupNames)
                    throws StorageException {
                for (String group : newGroupNames) {
                    addedFolders.add(course + "/" + assignmentName + "/" + group);
                }
            }
        };
        
        assertDoesNotThrow(() -> storage.createOrUpdateAssignmentsFromView(view));
        
        assertEquals(Set.of(
                "foo-123/Test01/student1",
                "foo-123/Test01/student2",
                "foo-123/Test02/student1",
                "foo-123/Test02/student2"
                // no folders for teacher
                ), addedFolders);
    }
    
    @Test
    public void createOrUpdateAssignmentsFromViewGroupAssignment() throws StuMgmtLoadingException {
        StuMgmtView view = new EmptyStuMgmtView() {
            @Override
            public void fullReload() {
                Course course = createCourse("foo-123");
                Assignment a1 = createAssignment(course, "123", "Test01",
                        StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                Assignment a2 = createAssignment(course, "321", "Test02",
                        StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                createGroup(a1, "abc", "Group01");
                createGroup(a1, "def", "Group02");
                createGroup(a2, "ghi", "Bar");
            }
        };
        view.fullReload();
        
        Set<String> addedFolders = new HashSet<>();
        ISubmissionStorage storage = new EmptyStorage() {
            @Override
            public void createOrUpdateAssignment(String course, String assignmentName, String... newGroupNames)
                    throws StorageException {
                for (String group : newGroupNames) {
                    addedFolders.add(course + "/" + assignmentName + "/" + group);
                }
            }
        };
        
        assertDoesNotThrow(() -> storage.createOrUpdateAssignmentsFromView(view));
        
        assertEquals(Set.of(
                "foo-123/Test01/Group01",
                "foo-123/Test01/Group02",
                "foo-123/Test02/Bar"
                ), addedFolders);
    }
    
    @Test
    public void createOrUpdateAssignmentsFromViewGroupOrSingleAssignment() throws StuMgmtLoadingException {
        StuMgmtView view = new EmptyStuMgmtView() {
            @Override
            public void fullReload() {
                Course course = createCourse("foo-123");
                createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                createParticipant(course, "def", "student2", RoleEnum.STUDENT);
                createParticipant(course, "ghi", "teacher", RoleEnum.LECTURER);
                
                Assignment a1 = createAssignment(course, "123", "Test01",
                        StateEnum.IN_PROGRESS, CollaborationEnum.GROUP_OR_SINGLE);
                Assignment a2 = createAssignment(course, "321", "Test02",
                        StateEnum.IN_PROGRESS, CollaborationEnum.GROUP_OR_SINGLE);
                createGroup(a1, "jkl", "Group01");
                createGroup(a1, "mno", "Group02");
                createGroup(a2, "pqr", "Bar");
            }
        };
        view.fullReload();
        
        Set<String> addedFolders = new HashSet<>();
        ISubmissionStorage storage = new EmptyStorage() {
            @Override
            public void createOrUpdateAssignment(String course, String assignmentName, String... newGroupNames)
                    throws StorageException {
                for (String group : newGroupNames) {
                    addedFolders.add(course + "/" + assignmentName + "/" + group);
                }
            }
        };
        
        assertDoesNotThrow(() -> storage.createOrUpdateAssignmentsFromView(view));
        
        assertEquals(Set.of(
                "foo-123/Test01/Group01",
                "foo-123/Test01/Group02",
                "foo-123/Test01/student1",
                "foo-123/Test01/student2",
                
                "foo-123/Test02/Bar",
                "foo-123/Test02/student1",
                "foo-123/Test02/student2"
                
                // no folders for teacher
                ), addedFolders);
    }
    
}
