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
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtLoadingException;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;

public class ISubmissionStorageTest {

    @Test
    public void createOrUpdateAssignmentsFromViewSingleAssignment() throws StuMgmtLoadingException {
        StuMgmtView view = new StuMgmtView(null, null, null, null) {
            @Override
            public void fullReload() {
                Course course = createCourse("foo-123");
                createAssignment(course, "Test01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                createAssignment(course, "Test02", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                createParticipant(course, "student1", RoleEnum.STUDENT);
                createParticipant(course, "student2", RoleEnum.STUDENT);
                createParticipant(course, "teacher", RoleEnum.LECTURER);
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
        StuMgmtView view = new StuMgmtView(null, null, null, null) {
            @Override
            public void fullReload() {
                Course course = createCourse("foo-123");
                Assignment a1 = createAssignment(course, "Test01", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                Assignment a2 = createAssignment(course, "Test02", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                createGroup(a1, "Group01");
                createGroup(a1, "Group02");
                createGroup(a2, "Bar");
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
        StuMgmtView view = new StuMgmtView(null, null, null, null) {
            @Override
            public void fullReload() {
                Course course = createCourse("foo-123");
                createParticipant(course, "student1", RoleEnum.STUDENT);
                createParticipant(course, "student2", RoleEnum.STUDENT);
                createParticipant(course, "teacher", RoleEnum.LECTURER);
                
                Assignment a1 = createAssignment(course, "Test01",
                        StateEnum.IN_PROGRESS, CollaborationEnum.GROUP_OR_SINGLE);
                Assignment a2 = createAssignment(course, "Test02",
                        StateEnum.IN_PROGRESS, CollaborationEnum.GROUP_OR_SINGLE);
                createGroup(a1, "Group01");
                createGroup(a1, "Group02");
                createGroup(a2, "Bar");
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
