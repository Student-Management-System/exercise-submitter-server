package net.ssehub.teaching.exercise_submitter.server.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.docker.StuMgmtDocker;
import net.ssehub.teaching.exercise_submitter.server.submission.UnauthorizedException;

public class AuthManagerIT {

    private static StuMgmtDocker docker;
    
    @BeforeAll
    public static void startServer() {
        docker = new StuMgmtDocker();
        
        docker.createUser("someuser", "abcdefgh");
    }
    
    @AfterAll
    public static void stopServer() {
        docker.close();
    }
    
    @Nested
    public class Authenticate {
        
        @Test
        public void invalidServerThrows() {
            AuthManager auth = new AuthManager("http://doesnt_exist.local");
            assertThrows(UnauthorizedException.class, () -> auth.authenticate("123"));
        }
        
        @Test
        public void invalidTokenThrows() {
            AuthManager auth = new AuthManager(docker.getAuthUrl());
            assertThrows(UnauthorizedException.class, () -> auth.authenticate("123"));
        }
        
        @Test
        public void validTokenReturnsUsername() {
            AuthManager auth = new AuthManager(docker.getAuthUrl());
            String user = assertDoesNotThrow(() -> auth.authenticate(docker.getAuthToken("someuser")));
            assertEquals("someuser", user);
        }
        
    }
    
}
