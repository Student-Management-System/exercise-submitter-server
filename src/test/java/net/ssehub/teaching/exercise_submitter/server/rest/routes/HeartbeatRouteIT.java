package net.ssehub.teaching.exercise_submitter.server.rest.routes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

public class HeartbeatRouteIT extends AbstractRestTest {

    @Test
    public void heartbeat() {
        startServer();
        Response response = target.path("/heartbeat").request().get();
        assertEquals(200, response.getStatus());
    }
    
}
