package net.ssehub.teaching.exercise_submitter.server.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.backend_api.ApiException;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.StateEnum;
import net.ssehub.studentmgmt.backend_api.model.ParticipantDto.RoleEnum;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Course;
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
                    "student", new SubmissionTarget("foo-123", "Homework01", "Group01")));
        }
        
        @Test
        public void nonExistingAssignmentNotAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student", RoleEnum.STUDENT);
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student", new SubmissionTarget("foo-123", "Homework01", "Group01")));
        }
        
        @Test
        public void nonExistingParticipantNotAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createAssignment(course, "Homework01", StateEnum.IN_PROGRESS);
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student", new SubmissionTarget("foo-123", "Homework01", "Group01")));
        }
        
        @Test
        public void userStudentAssignmentInProgressAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student", RoleEnum.STUDENT);
                    createAssignment(course, "Homework01", StateEnum.IN_PROGRESS);
                }
            });
            
            assertDoesNotThrow(() -> auth.checkSubmissionAllowed(
                    "student", new SubmissionTarget("foo-123", "Homework01", "Group01")));
        }
        
        @Test
        public void userStudentAssignmentInReviewNotAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student", RoleEnum.STUDENT);
                    createAssignment(course, "Homework01", StateEnum.IN_REVIEW);
                }
            });

            assertThrows(UnauthorizedException.class, () -> auth.checkSubmissionAllowed(
                    "student", new SubmissionTarget("foo-123", "Homework01", "Group01")));
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
                    "student", new SubmissionTarget("foo-123", "Homework01", "Group01")));
        }
        
        @Test
        public void nonExistingAssignmentNotAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student", RoleEnum.STUDENT);
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkReplayAllowed(
                    "student", new SubmissionTarget("foo-123", "Homework01", "Group01")));
        }
        
        @Test
        public void nonExistingParticipantNotAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createAssignment(course, "Homework01", StateEnum.IN_PROGRESS);
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkReplayAllowed(
                    "student", new SubmissionTarget("foo-123", "Homework01", "Group01")));
        }
        
        @Test
        public void userStudentAssignmentInProgressAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student", RoleEnum.STUDENT);
                    createAssignment(course, "Homework01", StateEnum.IN_PROGRESS);
                }
            });
            
            assertDoesNotThrow(() -> auth.checkReplayAllowed(
                    "student", new SubmissionTarget("foo-123", "Homework01", "Group01")));
        }
        
        @Test
        public void userStudentAssignmentInReviewNotAllowed() throws ApiException {
            
            AuthManager auth = new AuthManager("", new StuMgmtView(null) {
                @Override
                protected void init() throws ApiException {
                    Course course = createCourse("foo-123");
                    createParticipant(course, "student", RoleEnum.STUDENT);
                    createAssignment(course, "Homework01", StateEnum.IN_REVIEW);
                }
            });
            
            assertThrows(UnauthorizedException.class, () -> auth.checkReplayAllowed(
                    "student", new SubmissionTarget("foo-123", "Homework01", "Group01")));
        }
        
    }
    
}
