package net.ssehub.teaching.exercise_submitter.server.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.StateEnum;
import net.ssehub.studentmgmt.backend_api.model.ParticipantDto.RoleEnum;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Assignment;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Course;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.EmptyStuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Participant;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtLoadingException;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.submission.UnauthorizedException;

public class AuthManagerTest {

    @Nested
    public class CheckSubmissionAllowed {
        
        @Test
        public void nonExistingCourseNotAllowed() {
            AuthManager auth = new AuthManager("", new EmptyStuMgmtView());
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void nonExistingAssignmentNotAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void nonExistingParticipantNotAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createAssignment(course, "123", "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void userStudentAssignmentInProgressAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                    createAssignment(course, "123", "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertDoesNotThrow(() -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void userStudentAssignmentInReviewNotAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                    createAssignment(course, "123", "Homework01", StateEnum.IN_REVIEW, CollaborationEnum.SINGLE);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void singleAssignmentSubmitToSameUserAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                    createAssignment(course, "123", "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertDoesNotThrow(() -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void singleAssignmentSubmitToDifferentStudentNotAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                    createAssignment(course, "123", "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student2")));
        }
        
        @Test
        public void groupAssignmentSubmitToOwnGroupAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    Participant p = createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                    Assignment a = createAssignment(
                            course, "123", "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                    createGroup(a, "def", "Group01", p);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertDoesNotThrow(() -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "Group01")));
        }
        
        @Test
        public void groupAssignmentSubmitToDifferentGroupNotAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                    Assignment a = createAssignment(
                            course, "123", "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                    createGroup(a, "def", "Group01"); // student1 is not in this group
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "Group01")));
        }
        
        @Test
        public void groupAssignmentSubmitToNonExistantGroupNotAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                    createAssignment(course, "123", "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "doesnt_exist")));
        }
        
        @Test
        public void groupOrSingleAssignmentSubmitToSameUserAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                    createAssignment(course, "123", "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP_OR_SINGLE);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertDoesNotThrow(() -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void groupOrSingleAssignmentSubmitToDifferentStudentNotAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                    createAssignment(course, "123", "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP_OR_SINGLE);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student2")));
        }
        
        @Test
        public void groupOrSingleAssignmentSubmitToOwnGroupAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    Participant p = createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                    Assignment a = createAssignment(
                            course, "123", "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP_OR_SINGLE);
                    createGroup(a, "def", "Group01", p);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertDoesNotThrow(() -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "Group01")));
        }
        
        @Test
        public void groupOrSingleAssignmentSubmitToDifferentGroupNotAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                    Assignment a = createAssignment(
                            course, "123", "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP_OR_SINGLE);
                    createGroup(a, "def", "Group01"); // student1 is not in this group
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "Group01")));
        }
        
        @Test
        public void groupOrSingleAssignmentSubmitToNonExistantGroupNotAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                    createAssignment(course, "123", "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP_OR_SINGLE);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "doesnt_exist")));
        }
        
        @Test
        public void teacherCanSubmitToOtherUser() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                    createParticipant(course, "def", "teacher", RoleEnum.LECTURER);
                    createAssignment(course, "123", "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertDoesNotThrow(() -> auth.checkSubmissionAllowed(
                    "teacher", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
    }
    
    @Nested
    public class CheckReplayAllowed {
        
        @Test
        public void nonExistingCourseNotAllowed() throws StuMgmtLoadingException {
            
            AuthManager auth = new AuthManager("", new EmptyStuMgmtView());
            
            assertThrows(UnauthorizedException.class, () -> auth.checkReplayAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void nonExistingAssignmentNotAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertThrows(UnauthorizedException.class, () -> auth.checkReplayAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void nonExistingParticipantNotAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createAssignment(course, "123", "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertThrows(UnauthorizedException.class, () -> auth.checkReplayAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void userStudentAssignmentInProgressAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                    createAssignment(course, "123", "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertDoesNotThrow(() -> auth.checkReplayAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void userStudentAssignmentInReviewNotAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                    createAssignment(course, "123", "Homework01", StateEnum.IN_REVIEW, CollaborationEnum.SINGLE);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertThrows(UnauthorizedException.class, () -> auth.checkReplayAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student1")));
        }
        
        @Test
        public void singleAssignmentDifferentUserNotAllowed() throws StuMgmtLoadingException {
            StuMgmtView view = new EmptyStuMgmtView() {
                @Override
                public void fullReload() {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "abc", "student1", RoleEnum.STUDENT);
                    createAssignment(course, "123", "Homework01", StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                }
            };
            view.fullReload();
            AuthManager auth = new AuthManager("", view);
            
            assertThrows(UnauthorizedException.class, () -> auth.checkReplayAllowed(
                    "student1", new SubmissionTarget("foo-123", "Homework01", "student2")));
        }
        
    }
    
}
