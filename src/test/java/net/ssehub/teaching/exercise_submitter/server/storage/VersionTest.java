package net.ssehub.teaching.exercise_submitter.server.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

public class VersionTest {

    @Test
    public void author() {
        assertEquals("hans", new Version("hans", Instant.now()).getAuthor());
        assertEquals("peter", new Version("peter", Instant.now()).getAuthor());
    }
    
    @Test
    public void timestamp() {
        Instant t1 = Instant.parse("2021-10-20T14:30:10Z");
        Instant t2 = Instant.parse("2022-07-04T23:43:05Z");
        assertEquals(t1, new Version("hans", t1).getCreationTime());
        assertEquals(t2, new Version("hans", t2).getCreationTime());
    }
    
    @Test
    public void hashCodeEquals() {
        Version v1 = new Version("hans", Instant.parse("2021-10-20T14:30:10Z"));
        Version v2 = new Version("hans", Instant.parse("2021-10-20T14:30:10Z"));
        assertEquals(v1.hashCode(), v2.hashCode());
    }
    
    @Test
    public void equal() {
        Version v1 = new Version("hans", Instant.parse("2021-10-20T14:30:10Z"));
        Version v2 = new Version("hans", Instant.parse("2021-10-20T14:30:10Z"));
        assertTrue(v1.equals(v2));
        assertTrue(v2.equals(v1));
        assertTrue(v1.equals(v1));
    }
    
    @Test
    public void notEqual() {
        Version v1 = new Version("hans", Instant.parse("2021-10-20T14:30:10Z"));
        Version v2 = new Version("peter", Instant.parse("2021-10-20T14:30:10Z"));
        Version v3 = new Version("hans", Instant.parse("2022-07-04T23:43:05Z"));
        assertFalse(v1.equals(v2));
        assertFalse(v2.equals(v1));
        
        assertFalse(v1.equals(v3));
        assertFalse(v3.equals(v1));
        
        assertFalse(v1.equals(new Object()));
    }
    
    @Test
    public void toStringTest() {
        Version v1 = new Version("hans", Instant.parse("2021-10-20T14:30:10Z"));
        
        assertEquals("Version [author=hans, creationTime=1634740210]", v1.toString());
    }
    
}
