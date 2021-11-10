package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class CheckConfigurationTest {

    @Test
    public void checkNameStored() {
        CheckConfiguration c = new CheckConfiguration("checkstyle", false);
        assertEquals("checkstyle", c.getCheckName());
    }
    
    @Test
    public void rejectingStored() {
        CheckConfiguration c = new CheckConfiguration("javac", true);
        assertEquals(true, c.isRejecting());
    }
    
    @Test
    public void propertiesStored() {
        CheckConfiguration c = new CheckConfiguration("javac", false);
        c.setProperty("something", "test");
        
        assertAll(
            () -> assertEquals(Optional.of("test"), c.getProperty("something")),
            () -> assertEquals(Optional.empty(), c.getProperty("something else"))
        );
    }
    
    @Test
    public void equalsTrue() {
        CheckConfiguration c1 = new CheckConfiguration("javac", false);
        c1.setProperty("foo", "bar");
        CheckConfiguration c2 = new CheckConfiguration("javac", false);
        c2.setProperty("foo", "bar");
        
        assertAll(
            () -> assertEquals(c1, c1),
            () -> assertEquals(c1, c2)
        );
    }
    
    @Test
    public void equalsFalse() {
        CheckConfiguration c1 = new CheckConfiguration("javac", false);
        c1.setProperty("foo", "bar");
        
        CheckConfiguration c2 = new CheckConfiguration("checkstyle", false);
        c2.setProperty("foo", "bar");
        
        CheckConfiguration c3 = new CheckConfiguration("javac", true);
        c1.setProperty("foo", "bar");
        
        CheckConfiguration c4 = new CheckConfiguration("javac", true);
        c1.setProperty("foo2", "bar");
        
        assertAll(
            () -> assertNotEquals(c1, c2),
            () -> assertNotEquals(c1, c3),
            () -> assertNotEquals(c1, c4),
            () -> assertNotEquals(c1, new Object())
        );
    }
    
    @Test
    public void hashCodeEqualObjectSame() {
        CheckConfiguration c1 = new CheckConfiguration("javac", false);
        c1.setProperty("foo", "bar");
        CheckConfiguration c2 = new CheckConfiguration("javac", false);
        c2.setProperty("foo", "bar");
        
        assertEquals(c1.hashCode(), c2.hashCode());
    }
    
    @Test
    public void toStringOutput() {
        CheckConfiguration c1 = new CheckConfiguration("javac", false);
        c1.setProperty("foo", "bar");
        
        assertEquals("CheckConfiguration [checkName=javac, rejecting=false, properties={foo=bar}]", c1.toString());
    }
    
}
