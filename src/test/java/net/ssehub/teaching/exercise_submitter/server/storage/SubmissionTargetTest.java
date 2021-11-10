package net.ssehub.teaching.exercise_submitter.server.storage;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class SubmissionTargetTest {

    @Test
    public void courseStored() {
        SubmissionTarget target = new SubmissionTarget("some-course", "some-assignment", "some-group");
        assertEquals("some-course", target.getCourse());
    }
    
    @Test
    public void assignmentNameStored() {
        SubmissionTarget target = new SubmissionTarget("some-course", "some-assignment", "some-group");
        assertEquals("some-assignment", target.getAssignmentName());
    }
    
    @Test
    public void groupNameStored() {
        SubmissionTarget target = new SubmissionTarget("some-course", "some-assignment", "some-group");
        assertEquals("some-group", target.getGroupName());
    }
    
    @Test
    public void equalsTrue() {
        SubmissionTarget t1 = new SubmissionTarget("a", "b", "c");
        SubmissionTarget t2 = new SubmissionTarget("a", "b", "c");
        
        assertAll(
            () -> assertEquals(t1, t1),
            () -> assertEquals(t1, t2)
        );
    }
    
    @Test
    public void equalsFalse() {
        SubmissionTarget t1 = new SubmissionTarget("a", "b", "c");
        SubmissionTarget t2 = new SubmissionTarget("_", "b", "c");
        SubmissionTarget t3 = new SubmissionTarget("a", "_", "c");
        SubmissionTarget t4 = new SubmissionTarget("a", "b", "_");
        
        assertAll(
            () -> assertNotEquals(t1, t2),
            () -> assertNotEquals(t1, t3),
            () -> assertNotEquals(t1, t4),
            () -> assertNotEquals(t1, new Object())
        );
    }
    
    @Test
    public void hashCodeEqualObjectsSame() {
        SubmissionTarget t1 = new SubmissionTarget("a", "b", "c");
        SubmissionTarget t2 = new SubmissionTarget("a", "b", "c");
        
        assertEquals(t1.hashCode(), t2.hashCode());
    }
    
    @Test
    public void toStringOutput() {
        SubmissionTarget t1 = new SubmissionTarget("a", "b", "c");
        
        assertEquals("SubmissionTarget [course=a, assignmentName=b, groupName=c]", t1.toString());
    }
    
}
