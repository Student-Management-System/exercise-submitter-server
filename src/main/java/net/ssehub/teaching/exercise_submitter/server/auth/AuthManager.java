package net.ssehub.teaching.exercise_submitter.server.auth;

import net.ssehub.studentmgmt.sparkyservice_api.ApiClient;
import net.ssehub.studentmgmt.sparkyservice_api.ApiException;
import net.ssehub.studentmgmt.sparkyservice_api.api.AuthControllerApi;
import net.ssehub.studentmgmt.sparkyservice_api.model.AuthenticationInfoDto;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.submission.UnauthorizedException;

/**
 * Handles authentication and authorization of users.
 * 
 * @author Adam
 */
public class AuthManager {

    private String authApiUrl;
    
    /**
     * Creates a new authentication manager.
     * 
     * @param authApiUrl The URL to the authentication system (Sparky-Service) to check tokens.
     */
    public AuthManager(String authApiUrl) {
        this.authApiUrl = authApiUrl;
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
     * Checks if the given user is allowed to add a new submission to the given target.
     * 
     * @param user The name of the user that tries to add a new submission version.
     * @param target The target where the submission is supposed to go.
     * 
     * @throws UnauthorizedException If the user is not allowed to submit.
     */
    public void checkSubmissionAllowed(String user, SubmissionTarget target) throws UnauthorizedException {
        // TODO
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
        // TODO
    }
    
}
