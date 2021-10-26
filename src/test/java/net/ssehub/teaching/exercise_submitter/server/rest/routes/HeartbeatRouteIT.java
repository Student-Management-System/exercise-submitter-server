package net.ssehub.teaching.exercise_submitter.server.rest.routes;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

public class HeartbeatRouteIT extends AbstractRestTest {

    @Test
    public void heartbeat() {
        startServer();
        Response response = target.path("/heartbeat").request().get();
        Map<?, ?> result = response.readEntity(Map.class);
        
        assertAll(
            () -> assertEquals(200, response.getStatus()),
            () -> assertEquals("ok", result.get("status"))
        );
        
    }
    
}
