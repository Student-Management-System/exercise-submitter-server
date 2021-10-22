package net.ssehub.teaching.exercise_submitter.server.auth;

import net.ssehub.studentmgmt.backend_api.model.ParticipantDto.RoleEnum;
import net.ssehub.studentmgmt.sparkyservice_api.ApiClient;
import net.ssehub.studentmgmt.sparkyservice_api.ApiException;
import net.ssehub.studentmgmt.sparkyservice_api.api.AuthControllerApi;
import net.ssehub.studentmgmt.sparkyservice_api.model.AuthenticationInfoDto;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Assignment;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Course;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Participant;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.submission.UnauthorizedException;

/**
 * Handles authentication and authorization of users.
 * 
 * @author Adam
 */
public class AuthManager {

    private String authApiUrl;
    
    private StuMgmtView stuMgmtView;
    
    /**
     * Creates a new authentication manager.
     * 
     * @param authApiUrl The URL to the authentication system (Sparky-Service) to check tokens.
     * @param stuMgmtView The view on the student management system.
     */
    public AuthManager(String authApiUrl, StuMgmtView stuMgmtView) {
        this.authApiUrl = authApiUrl;
        this.stuMgmtView = stuMgmtView;
    }
    
    /**
     * Authenticates a user based on a JWT token.
     * 
     * @param token The JWT token.
     * 
     * @return The name of the authorized user.
     * 
     * @throws UnauthorizedException If the user cannot be authorized.
     */
    public String authenticate(String token) throws UnauthorizedException {
        ApiClient client = new ApiClient();
        client.setBasePath(authApiUrl);
        client.setAccessToken(token);
        
        AuthControllerApi api = new AuthControllerApi(client);
        AuthenticationInfoDto dto;
        try {
            dto = api.checkTokenAuthenticationStatus();
        } catch (ApiException e) {
            throw new UnauthorizedException(e);
        }
        
        return dto.getUser().getUsername(); 
    }
    
    /**
     * Checks if the given participant is allowed to submit to the given group name for the given assignment.
     * 
     * @param assignment The assignment that the user tries to submit to.
     * @param participant The participant that tries to submit.
     * @param targetGroupName The name of the group diretory that the user tries to submit to.
     * 
     * @throws UnauthorizedException If the user is not allowed to submit there.
     */
    private void checkIfParticipantCanSubmitToGroup(
            Assignment assignment, Participant participant, String targetGroupName) throws UnauthorizedException {
        
        if (participant.getRole() == RoleEnum.STUDENT) {
            boolean groupAllowed;
            switch (assignment.getCollaboration()) {
            case SINGLE:
                groupAllowed = targetGroupName.equals(participant.getName());
                break;
                
            case GROUP:
                groupAllowed = assignment.getGroup(targetGroupName)
                        .orElseThrow(() -> new UnauthorizedException())
                        .hasParticipant(participant);
                break;
            
            case GROUP_OR_SINGLE:
                groupAllowed = targetGroupName.equals(participant.getName())
                        || assignment.getGroup(targetGroupName)
                            .orElseThrow(() -> new UnauthorizedException())
                            .hasParticipant(participant);
                break;
                
            default:
                groupAllowed = false;
                break;            
            }
            
            if (!groupAllowed) {
                throw new UnauthorizedException();
            }
        }
    }
    
    /**
     * Checks if the given user is allowed to add a new submission to the given target.
     * 
     * @param user The name of the user that tries to add a new submission version.
     * @param target The target where the submission is supposed to go.
     * 
     * @throws UnauthorizedException If the user is not allowed to submit.
     */
    public void checkSubmissionAllowed(String user, SubmissionTarget target) throws UnauthorizedException {
        Course course = stuMgmtView.getCourse(target.getCourse())
                .orElseThrow(() -> new UnauthorizedException());
        
        Assignment assignment = course.getAssignment(target.getAssignmentName())
                .orElseThrow(() -> new UnauthorizedException());
        
        Participant participant = course.getParticipant(user)
                .orElseThrow(() -> new UnauthorizedException());
        
        if (!assignment.canSubmit(participant)) {
            throw new UnauthorizedException();
        }
        
        checkIfParticipantCanSubmitToGroup(assignment, participant, target.getGroupName());
    }
    
    /**
     * Checks if the given user is allowed to replay a previous submission of the given target.
     * 
     * @param user The name of the user that tries to replay a previous submission version.
     * @param target The target identifying the submission.
     * 
     * @throws UnauthorizedException If the user is not allowed to submit.
     */
    public void checkReplayAllowed(String user, SubmissionTarget target) throws UnauthorizedException {
        Course course = stuMgmtView.getCourse(target.getCourse())
                .orElseThrow(() -> new UnauthorizedException());
        
        Assignment assignment = course.getAssignment(target.getAssignmentName())
                .orElseThrow(() -> new UnauthorizedException());
        
        Participant participant = course.getParticipant(user)
                .orElseThrow(() -> new UnauthorizedException());
        
        if (!assignment.canReplay(participant)) {
            throw new UnauthorizedException();
        }
        
        checkIfParticipantCanSubmitToGroup(assignment, participant, target.getGroupName());
    }
    
}
