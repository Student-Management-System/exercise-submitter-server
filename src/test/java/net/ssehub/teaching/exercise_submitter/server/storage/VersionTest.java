package net.ssehub.teaching.exercise_submitter.server.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

public class VersionTest {

    @Test
    public void author() {
        assertEquals("hans", new Version("hans", LocalDateTime.now()).getAuthor());
        assertEquals("peter", new Version("peter", LocalDateTime.now()).getAuthor());
    }
    
    @Test
    public void timestamp() {
        LocalDateTime t1 = LocalDateTime.of(2021, 10, 20, 14, 30, 10);
        LocalDateTime t2 = LocalDateTime.of(2022, 7, 4, 23, 43, 5);
        assertEquals(t1, new Version("hans", t1).getTimestamp());
        assertEquals(t2, new Version("hans", t2).getTimestamp());
    }
    
    @Test
    public void hashCodeEquals() {
        Version v1 = new Version("hans", LocalDateTime.of(2021, 10, 20, 14, 30, 10));
        Version v2 = new Version("hans", LocalDateTime.of(2021, 10, 20, 14, 30, 10));
        assertEquals(v1.hashCode(), v2.hashCode());
    }
    
    @Test
    public void equal() {
        Version v1 = new Version("hans", LocalDateTime.of(2021, 10, 20, 14, 30, 10));
        Version v2 = new Version("hans", LocalDateTime.of(2021, 10, 20, 14, 30, 10));
        assertTrue(v1.equals(v2));
        assertTrue(v2.equals(v1));
        assertTrue(v1.equals(v1));
    }
    
    @Test
    public void notEqual() {
        Version v1 = new Version("hans", LocalDateTime.of(2021, 10, 20, 14, 30, 10));
        Version v2 = new Version("peter", LocalDateTime.of(2021, 10, 20, 14, 30, 10));
        Version v3 = new Version("hans", LocalDateTime.of(2022, 7, 4, 23, 43, 5));
        assertFalse(v1.equals(v2));
        assertFalse(v2.equals(v1));
        
        assertFalse(v1.equals(v3));
        assertFalse(v3.equals(v1));
        
        assertFalse(v1.equals(new Object()));
    }
    
    @Test
    public void toStringTest() {
        Version v1 = new Version("hans", LocalDateTime.of(2021, 10, 20, 14, 30, 10));
        
        assertEquals("Version [author=hans, timestamp=2021-10-20T14:30:10]", v1.toString());
    }
    
}
