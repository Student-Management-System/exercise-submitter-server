package net.ssehub.teaching.exercise_submitter.server.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.backend_api.ApiException;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.StateEnum;
import net.ssehub.studentmgmt.backend_api.model.ParticipantDto.RoleEnum;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Assignment;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Course;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Participant;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.submission.UnauthorizedException;

public class AuthManagerTest {

    @Nested
    public class CheckSubmissionAllowed {
        
        @Test
        public void nonExistingCourseNotAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void nonExistingAssignmentNotAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student1", RoleEnum.STUDENT);
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void nonExistingParticipantNotAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createAssignment(course, "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void userStudentAssignmentInProgressAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student1", RoleEnum.STUDENT);
                    createAssignment(course, "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                }
            });
            
            assertDoesNotThrow(() -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void userStudentAssignmentInReviewNotAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student1", RoleEnum.STUDENT);
                    createAssignment(course, "Homework01", StateEnum.IN_REVIEW, CollaborationEnum.SINGLE);
                }
            });

            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void singleAssignmentSubmitToSameUserAllowed() throws ApiException {
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student1", RoleEnum.STUDENT);
                    createAssignment(course, "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                }
            });
            
            assertDoesNotThrow(() -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void singleAssignmentSubmitToDifferentStudentNotAllowed() throws ApiException {
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student1", RoleEnum.STUDENT);
                    createAssignment(course, "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student2")));
        }
        
        @Test
        public void groupAssignmentSubmitToOwnGroupAllowed() throws ApiException {
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    Participant p = createParticipant(course, "student1", RoleEnum.STUDENT);
                    Assignment a = createAssignment(
                            course, "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                    createGroup(a, "Group01", p);
                }
            });
            
            assertDoesNotThrow(() -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "Group01")));
        }
        
        @Test
        public void groupAssignmentSubmitToDifferentGroupNotAllowed() throws ApiException {
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student1", RoleEnum.STUDENT);
                    Assignment a = createAssignment(
                            course, "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                    createGroup(a, "Group01"); // student1 is not in this group
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "Group01")));
        }
        
        @Test
        public void groupAssignmentSubmitToNonExistantGroupNotAllowed() throws ApiException {
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student1", RoleEnum.STUDENT);
                    createAssignment(course, "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "doesnt_exist")));
        }
        
        @Test
        public void groupOrSingleAssignmentSubmitToSameUserAllowed() throws ApiException {
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student1", RoleEnum.STUDENT);
                    createAssignment(course, "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP_OR_SINGLE);
                }
            });
            
            assertDoesNotThrow(() -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void groupOrSingleAssignmentSubmitToDifferentStudentNotAllowed() throws ApiException {
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student1", RoleEnum.STUDENT);
                    createAssignment(course, "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP_OR_SINGLE);
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student2")));
        }
        
        @Test
        public void groupOrSingleAssignmentSubmitToOwnGroupAllowed() throws ApiException {
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    Participant p = createParticipant(course, "student1", RoleEnum.STUDENT);
                    Assignment a = createAssignment(
                            course, "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP_OR_SINGLE);
                    createGroup(a, "Group01", p);
                }
            });
            
            assertDoesNotThrow(() -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "Group01")));
        }
        
        @Test
        public void groupOrSingleAssignmentSubmitToDifferentGroupNotAllowed() throws ApiException {
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student1", RoleEnum.STUDENT);
                    Assignment a = createAssignment(
                            course, "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP_OR_SINGLE);
                    createGroup(a, "Group01"); // student1 is not in this group
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "Group01")));
        }
        
        @Test
        public void groupOrSingleAssignmentSubmitToNonExistantGroupNotAllowed() throws ApiException {
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student1", RoleEnum.STUDENT);
                    createAssignment(course, "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP_OR_SINGLE);
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "doesnt_exist")));
        }
        
        @Test
        public void teacherCanSubmitToOtherUser() throws ApiException {
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student1", RoleEnum.STUDENT);
                    createParticipant(course, "teacher", RoleEnum.LECTURER);
                    createAssignment(course, "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                }
            });
            
            assertDoesNotThrow(() -> auth.checkSubmissionAllowed(
                    "teacher", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
    }
    
    @Nested
    public class CheckReplayAllowed {
        
        @Test
        public void nonExistingCourseNotAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkReplayAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void nonExistingAssignmentNotAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student1", RoleEnum.STUDENT);
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkReplayAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void nonExistingParticipantNotAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createAssignment(course, "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkReplayAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void userStudentAssignmentInProgressAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student1", RoleEnum.STUDENT);
                    createAssignment(course, "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                }
            });
            
            assertDoesNotThrow(() -> auth.checkReplayAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void userStudentAssignmentInReviewNotAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student1", RoleEnum.STUDENT);
                    createAssignment(course, "Homework01", StateEnum.IN_REVIEW, CollaborationEnum.SINGLE);
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkReplayAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void singleAssignmentDifferentUserNotAllowed() throws ApiException {
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student1", RoleEnum.STUDENT);
                    createAssignment(course, "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkReplayAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student2")));
        }
        
        
    }
    
}
