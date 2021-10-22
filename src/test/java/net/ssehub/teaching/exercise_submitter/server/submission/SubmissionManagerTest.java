package net.ssehub.teaching.exercise_submitter.server.submission;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.server.checks.Check;
import net.ssehub.teaching.exercise_submitter.server.checks.ResultMessage;
import net.ssehub.teaching.exercise_submitter.server.checks.ResultMessage.MessageType;
import net.ssehub.teaching.exercise_submitter.server.rest.dto.CheckMessageDto;
import net.ssehub.teaching.exercise_submitter.server.rest.dto.SubmissionResultDto;
import net.ssehub.teaching.exercise_submitter.server.storage.EmptyStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionBuilder;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;

public class SubmissionManagerTest {

    @Test
    public void failedRejectinCheckRejects() {
        SubmissionManager manager = new SubmissionManager(new EmptyStorage());
        manager.addRejectingCheck(new Check() {
            @Override
            public boolean run(File submissionDirectory) {
                addResultMessage(new ResultMessage("test", MessageType.ERROR, "mock"));
                return false;
            }
        });
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "a", "g"), new SubmissionBuilder("s").build()));
        
        assertAll(
            () -> assertFalse(result.getAccepted()),
            () -> assertEquals(
                    Arrays.asList(new CheckMessageDto("test", MessageType.ERROR, "mock")), result.getMessages())
        );
    }
    
    @Test
    public void notFailedRejectinCheckDoesNotReject() {
        SubmissionManager manager = new SubmissionManager(new EmptyStorage());
        manager.addRejectingCheck(new Check() {
            @Override
            public boolean run(File submissionDirectory) {
                addResultMessage(new ResultMessage("test", MessageType.WARNING, "mock"));
                return true;
            }
        });
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "a", "g"), new SubmissionBuilder("s").build()));
        
        assertAll(
            () -> assertTrue(result.getAccepted()),
            () -> assertEquals(
                    Arrays.asList(new CheckMessageDto("test", MessageType.WARNING, "mock")), result.getMessages())
        );
    }
    
    @Test
    public void failedNonRejectinCheckDoesNotReject() {
        SubmissionManager manager = new SubmissionManager(new EmptyStorage());
        manager.addNonRejectingCheck(new Check() {
            @Override
            public boolean run(File submissionDirectory) {
                addResultMessage(new ResultMessage("test", MessageType.ERROR, "mock"));
                return false;
            }
        });
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "a", "g"), new SubmissionBuilder("s").build()));
        
        assertAll(
            () -> assertTrue(result.getAccepted()),
            () -> assertEquals(
                    Arrays.asList(new CheckMessageDto("test", MessageType.ERROR, "mock")), result.getMessages())
        );
    }
    
    @Test
    public void failedRejectinBlocksFurtherChecks() {
        SubmissionManager manager = new SubmissionManager(new EmptyStorage());
        manager.addRejectingCheck(new Check() {
            @Override
            public boolean run(File submissionDirectory) {
                addResultMessage(new ResultMessage("one", MessageType.ERROR, "mock"));
                return false;
            }
        });
        manager.addRejectingCheck(new Check() {
            @Override
            public boolean run(File submissionDirectory) {
                addResultMessage(new ResultMessage("two", MessageType.ERROR, "mock"));
                return false;
            }
        });
        manager.addNonRejectingCheck(new Check() {
            @Override
            public boolean run(File submissionDirectory) {
                addResultMessage(new ResultMessage("three", MessageType.ERROR, "mock"));
                return false;
            }
        });
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "a", "g"), new SubmissionBuilder("s").build()));
        
         assertEquals(Arrays.asList(new CheckMessageDto("one", MessageType.ERROR, "mock")), result.getMessages());
    }
    
    @Test
    public void failedNonRejectinDoesNotBlockFurtherChecks() {
        SubmissionManager manager = new SubmissionManager(new EmptyStorage());
        manager.addNonRejectingCheck(new Check() {
            @Override
            public boolean run(File submissionDirectory) {
                addResultMessage(new ResultMessage("one", MessageType.ERROR, "mock"));
                return false;
            }
        });
        manager.addNonRejectingCheck(new Check() {
            @Override
            public boolean run(File submissionDirectory) {
                addResultMessage(new ResultMessage("two", MessageType.ERROR, "mock"));
                return false;
            }
        });
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "a", "g"), new SubmissionBuilder("s").build()));
        
        assertEquals(Arrays.asList(
                new CheckMessageDto("one", MessageType.ERROR, "mock"),
                new CheckMessageDto("two", MessageType.ERROR, "mock")
                ), result.getMessages());
    }
    
    @Test
    public void nonFailedRejectinDoesNotBlockFurtherChecks() {
        SubmissionManager manager = new SubmissionManager(new EmptyStorage());
        manager.addRejectingCheck(new Check() {
            @Override
            public boolean run(File submissionDirectory) {
                addResultMessage(new ResultMessage("one", MessageType.ERROR, "mock"));
                return true;
            }
        });
        manager.addRejectingCheck(new Check() {
            @Override
            public boolean run(File submissionDirectory) {
                addResultMessage(new ResultMessage("two", MessageType.ERROR, "mock"));
                return true;
            }
        });
        manager.addNonRejectingCheck(new Check() {
            @Override
            public boolean run(File submissionDirectory) {
                addResultMessage(new ResultMessage("three", MessageType.ERROR, "mock"));
                return true;
            }
        });
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "a", "g"), new SubmissionBuilder("s").build()));
        
         assertEquals(Arrays.asList(
                 new CheckMessageDto("one", MessageType.ERROR, "mock"),
                 new CheckMessageDto("three", MessageType.ERROR, "mock"),
                 new CheckMessageDto("two", MessageType.ERROR, "mock")
                 ), result.getMessages());
    }
    
}
