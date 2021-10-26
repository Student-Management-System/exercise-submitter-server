package net.ssehub.teaching.exercise_submitter.server.auth;

import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.EmptyStuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.submission.UnauthorizedException;

public class PermissiveAuthManager extends AuthManager {

    public PermissiveAuthManager() {
        super("", new EmptyStuMgmtView());
    }

    public static String generateUsername(String token) {
        return "user_" + token;
    }
    
    @Override
    public String authenticate(String token) throws UnauthorizedException {
        return generateUsername(token);
    }
    
    @Override
    public void checkReplayAllowed(String user, SubmissionTarget target) throws UnauthorizedException {
    }
    
    @Override
    public void checkSubmissionAllowed(String user, SubmissionTarget target) throws UnauthorizedException {
    }
    
}
