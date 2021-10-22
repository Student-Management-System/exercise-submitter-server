package net.ssehub.teaching.exercise_submitter.server.auth;

import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.submission.UnauthorizedException;

/**
 * Handles authentication and authorization of users.
 * 
 * @author Adam
 */
public class AuthManager {

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
        return "user"; // TODO 
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
