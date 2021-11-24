package net.ssehub.teaching.exercise_submitter.server.submission;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.StateEnum;
import net.ssehub.teaching.exercise_submitter.server.rest.dto.CheckMessageDto;
import net.ssehub.teaching.exercise_submitter.server.rest.dto.SubmissionResultDto;
import net.ssehub.teaching.exercise_submitter.server.storage.EmptyStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchTargetException;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.Submission;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionBuilder;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.storage.Version;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Assignment;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.CheckConfiguration;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Course;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.EmptyStuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtLoadingException;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.Check;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.CheckstyleCheck;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.EncodingCheck;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.JavacCheck;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;

public class SubmissionManagerTest {

    private static class MockCheck extends Check {

        private boolean rejecting;
        
        private boolean returnValue;
        
        private ResultMessage[] messages;
        
        
        public MockCheck(boolean rejecting, boolean returnValue, ResultMessage... messages) {
            this.rejecting = rejecting;
            this.returnValue = returnValue;
            this.messages = messages;
        }
        
        @Override
        public boolean run(Path submissionDirectory) {
            Arrays.stream(messages).forEach(this::addResultMessage);
            return returnValue;
        }
        
    }
    
    private static String createJsonString(MockCheck... checks) {
        StringBuilder s = new StringBuilder();
        s.append("[");
        
        for (MockCheck check : checks) {
            s.append("{\"check\":\"mock\",\"rejecting\":").append(check.rejecting)
                .append(",\"returnValue\":\"").append(check.returnValue).append("\"")
                .append(",\"numMessages\":\"").append(Integer.toString(check.messages.length)).append("\"");
            
            for (int i = 0; i < check.messages.length; i++) {
                ResultMessage m = check.messages[i];
                s.append(",\"m").append(i).append("Check\":\"").append(m.getCheckName()).append("\"");
                s.append(",\"m").append(i).append("Level\":\"").append(m.getType().name()).append("\"");
                s.append(",\"m").append(i).append("Msg\":\"").append(m.getMessage()).append("\"");
            }
            s.append("},");
        }
        if (s.length() > 1) { // delete trailing comma
            s.delete(s.length() - 1, s.length());
        }
        
        s.append("]");
        return s.toString();
    }
    
    private static class TestSubmissionManager extends SubmissionManager {

        public TestSubmissionManager(ISubmissionStorage storage, StuMgmtView stuMgmtView) {
            super(storage, stuMgmtView);
            assertDoesNotThrow(() -> stuMgmtView.fullReload());
        }
        
        @Override
        protected Check createCheck(CheckConfiguration checkConfiguration) throws IllegalArgumentException {
            if (checkConfiguration.getCheckName().equals("mock")) {
                MockCheck check = new MockCheck(
                        checkConfiguration.isRejecting(),
                        Boolean.parseBoolean(checkConfiguration.getProperty("returnValue").get()));
                check.messages = new ResultMessage[Integer.parseInt(checkConfiguration.getProperty("numMessages").get())];
                
                for (int i = 0; i < check.messages.length; i++) {
                    check.messages[i] = new ResultMessage(
                            checkConfiguration.getProperty("m" + i + "Check").get(),
                            ResultMessage.MessageType.valueOf(checkConfiguration.getProperty("m" + i + "Level").get()),
                            checkConfiguration.getProperty("m" + i + "Msg").get());
                }
                
                return check;
            }
            return super.createCheck(checkConfiguration);
        }
    }
    
    @Test
    public void failedRejectingCheckRejects() {
        AtomicBoolean submissionStored = new AtomicBoolean(false);
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage() {
                @Override
                public void submitNewVersion(SubmissionTarget target, Submission submission)
                        throws NoSuchTargetException, StorageException {
                    submissionStored.set(true);
                }
            }, new EmptyStuMgmtView() {
                @Override
                public void fullReload() throws StuMgmtLoadingException {
                    Course c = createCourse("c");
                    Assignment a = createAssignment(c, "a", "a", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                    a.setCheckConfigurationString(createJsonString(
                            new MockCheck(true, false, new ResultMessage("test", MessageType.ERROR, "mock"))
                    ));
                }
            });
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "a", "g"), new SubmissionBuilder("s").build()));
        
        assertAll(
            () -> assertFalse(result.getAccepted()),
            () -> assertEquals(
                    Arrays.asList(new CheckMessageDto("test", MessageType.ERROR, "mock")), result.getMessages()),
            () -> assertFalse(submissionStored.get())
        );
    }
    
    @Test
    public void notFailedRejectingCheckDoesNotReject() {
        AtomicBoolean submissionStored = new AtomicBoolean(false);
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage() {
                @Override
                public void submitNewVersion(SubmissionTarget target, Submission submission)
                        throws NoSuchTargetException, StorageException {
                    submissionStored.set(true);
                }
            }, new EmptyStuMgmtView() {
                @Override
                public void fullReload() throws StuMgmtLoadingException {
                    Course c = createCourse("c");
                    Assignment a = createAssignment(c, "a", "a", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                    a.setCheckConfigurationString(createJsonString(
                            new MockCheck(true, true, new ResultMessage("test", MessageType.WARNING, "mock"))
                    ));
                }
            });
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "a", "g"), new SubmissionBuilder("s").build()));
        
        assertAll(
            () -> assertTrue(result.getAccepted()),
            () -> assertEquals(
                    Arrays.asList(new CheckMessageDto("test", MessageType.WARNING, "mock")), result.getMessages()),
            () -> assertTrue(submissionStored.get())
        );
    }
    
    @Test
    public void failedDefaultRejectingCheckRejects() {
        AtomicBoolean submissionStored = new AtomicBoolean(false);
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage() {
                @Override
                public void submitNewVersion(SubmissionTarget target, Submission submission)
                        throws NoSuchTargetException, StorageException {
                    submissionStored.set(true);
                }
            }, new EmptyStuMgmtView() {
                @Override
                public void fullReload() throws StuMgmtLoadingException {
                    Course c = createCourse("c");
                    createAssignment(c, "a", "a", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                }
            });
        
        manager.addDefaultRejectingCheck(new MockCheck(true, false, new ResultMessage("test", MessageType.ERROR, "mock")));
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "a", "g"), new SubmissionBuilder("s").build()));
        
        assertAll(
            () -> assertFalse(result.getAccepted()),
            () -> assertEquals(
                    Arrays.asList(new CheckMessageDto("test", MessageType.ERROR, "mock")), result.getMessages()),
            () -> assertFalse(submissionStored.get())
        );
    }
    
    @Test
    public void notFailedDefaultRejectingCheckDoesNotReject() {
        AtomicBoolean submissionStored = new AtomicBoolean(false);
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage() {
                @Override
                public void submitNewVersion(SubmissionTarget target, Submission submission)
                        throws NoSuchTargetException, StorageException {
                    submissionStored.set(true);
                }
            }, new EmptyStuMgmtView() {
                @Override
                public void fullReload() throws StuMgmtLoadingException {
                    Course c = createCourse("c");
                    createAssignment(c, "a", "a", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                }
            });
        
        manager.addDefaultRejectingCheck(new MockCheck(true, true, new ResultMessage("test", MessageType.WARNING, "mock")));
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "a", "g"), new SubmissionBuilder("s").build()));
        
        assertAll(
            () -> assertTrue(result.getAccepted()),
            () -> assertEquals(
                    Arrays.asList(new CheckMessageDto("test", MessageType.WARNING, "mock")), result.getMessages()),
            () -> assertTrue(submissionStored.get())
        );
    }
    
    
    @Test
    public void failedNonRejectingCheckDoesNotReject() {
        AtomicBoolean submissionStored = new AtomicBoolean(false);
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage() {
            @Override
            public void submitNewVersion(SubmissionTarget target, Submission submission)
                    throws NoSuchTargetException, StorageException {
                submissionStored.set(true);
            }
        }, new EmptyStuMgmtView() {
            @Override
            public void fullReload() throws StuMgmtLoadingException {
                Course c = createCourse("c");
                Assignment a = createAssignment(c, "a", "a", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                a.setCheckConfigurationString(createJsonString(
                    new MockCheck(false, false, new ResultMessage("test", MessageType.ERROR, "mock"))
                ));
            }
        });
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "a", "g"), new SubmissionBuilder("s").build()));
        
        assertAll(
            () -> assertTrue(result.getAccepted()),
            () -> assertEquals(
                    Arrays.asList(new CheckMessageDto("test", MessageType.ERROR, "mock")), result.getMessages()),
            () -> assertTrue(submissionStored.get())
        );
    }
    
    @Test
    public void failedRejectinBlocksFurtherChecks() {
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage(), new EmptyStuMgmtView() {
            @Override
            public void fullReload() throws StuMgmtLoadingException {
                Course c = createCourse("c");
                Assignment a = createAssignment(c, "a", "a", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                a.setCheckConfigurationString(createJsonString(
                        new MockCheck(true, false, new ResultMessage("one", MessageType.ERROR, "mock")),
                        new MockCheck(true, false, new ResultMessage("two", MessageType.ERROR, "mock")),
                        new MockCheck(false, false, new ResultMessage("three", MessageType.ERROR, "mock"))
                ));
            }
        });
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "a", "g"), new SubmissionBuilder("s").build()));
        
         assertEquals(Arrays.asList(new CheckMessageDto("one", MessageType.ERROR, "mock")), result.getMessages());
    }
    
    @Test
    public void failedNonRejectinDoesNotBlockFurtherChecks() {
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage(), new EmptyStuMgmtView() {
            @Override
            public void fullReload() throws StuMgmtLoadingException {
                Course c = createCourse("c");
                Assignment a = createAssignment(c, "a", "a", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                a.setCheckConfigurationString(createJsonString(
                        new MockCheck(false, false, new ResultMessage("one", MessageType.ERROR, "mock")),
                        new MockCheck(false, false, new ResultMessage("two", MessageType.ERROR, "mock"))
                ));
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
    public void nonFailedRejectingDoesNotBlockFurtherChecks() {
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage(), new EmptyStuMgmtView() {
            @Override
            public void fullReload() throws StuMgmtLoadingException {
                Course c = createCourse("c");
                Assignment a = createAssignment(c, "a", "a", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                a.setCheckConfigurationString(createJsonString(
                        new MockCheck(true, true, new ResultMessage("one", MessageType.ERROR, "mock")),
                        new MockCheck(true, true, new ResultMessage("two", MessageType.ERROR, "mock")),
                        new MockCheck(false, true, new ResultMessage("three", MessageType.ERROR, "mock"))
                ));
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
    
    @Test
    public void acceptedSubmissionSendsResultMessagesToStuMgmt() {
        List<ResultMessage> sentMessages = new LinkedList<>();
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage(), new EmptyStuMgmtView() {
            @Override
            public void sendSubmissionResult(SubmissionTarget target, List<ResultMessage> messages) {
                sentMessages.addAll(messages);
            }
            @Override
            public void fullReload() throws StuMgmtLoadingException {
                Course c = createCourse("a");
                Assignment a = createAssignment(c, "b", "b", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                a.setCheckConfigurationString(createJsonString(
                        new MockCheck(false, false, new ResultMessage("mock", MessageType.WARNING, "mock message 1"),
                                new ResultMessage("mock", MessageType.WARNING, "mock message 2"))
                ));
            }
        });
        
        assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("a", "b", "c"), new SubmissionBuilder("s").build()));
        
        assertEquals(Arrays.asList(
                new ResultMessage("mock", MessageType.WARNING, "mock message 1"),
                new ResultMessage("mock", MessageType.WARNING, "mock message 2")), sentMessages);
    }
    
    @Test
    public void rejectedSubmissionDoesNotSendResultMessagesToStuMgmt() {
        List<ResultMessage> sentMessages = new LinkedList<>();
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage(), new EmptyStuMgmtView() {
            @Override
            public void sendSubmissionResult(SubmissionTarget target, List<ResultMessage> messages) {
                sentMessages.addAll(messages);
            }
            @Override
            public void fullReload() throws StuMgmtLoadingException {
                Course c = createCourse("a");
                Assignment a = createAssignment(c, "b", "b", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                a.setCheckConfigurationString(createJsonString(
                        new MockCheck(true, false, new ResultMessage("mock", MessageType.WARNING, "mock message 1"),
                                new ResultMessage("mock", MessageType.WARNING, "mock message 2"))
                ));
            }
        });
        
        assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("a", "b", "c"), new SubmissionBuilder("s").build()));
        
        assertEquals(Collections.emptyList(), sentMessages);
    }
    
    @Test
    public void noResultMessagesCallSendResultMessage() {
        AtomicBoolean called = new AtomicBoolean(false);
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage(), new EmptyStuMgmtView() {
            @Override
            public void sendSubmissionResult(SubmissionTarget target, List<ResultMessage> messages) {
                called.set(true);
            }
            @Override
            public void fullReload() throws StuMgmtLoadingException {
                Course c = createCourse("a");
                Assignment a = createAssignment(c, "b", "b", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                a.setCheckConfigurationString(createJsonString(
                        new MockCheck(false, true)
                ));
            }
        });
        
        assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("a", "b", "c"), new SubmissionBuilder("s").build()));
        
        assertTrue(called.get());
    }
    
    @Test
    public void onlyDefaultRejectingChecksDontCallSendResultMessage() {
        AtomicBoolean called = new AtomicBoolean(false);
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage(), new EmptyStuMgmtView() {
            @Override
            public void fullReload() throws StuMgmtLoadingException {
                Course c = createCourse("a");
                createAssignment(c, "b", "b", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
            }
            @Override
            public void sendSubmissionResult(SubmissionTarget target, List<ResultMessage> messages) {
                called.set(true);
            }
        });
        
        manager.addDefaultRejectingCheck(new MockCheck(true, true));
        
        assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("a", "b", "c"), new SubmissionBuilder("s").build()));
        
        assertFalse(called.get());
    }
    
    @Test
    public void nonDefaultRejectingChecksCallsSendResultMessage() {
        AtomicBoolean called = new AtomicBoolean(false);
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage(), new EmptyStuMgmtView() {
            @Override
            public void fullReload() throws StuMgmtLoadingException {
                Course c = createCourse("a");
                Assignment a = createAssignment(c, "b", "b", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                a.setCheckConfigurationString(createJsonString(
                    new MockCheck(true, true)
                ));
            }
            @Override
            public void sendSubmissionResult(SubmissionTarget target, List<ResultMessage> messages) {
                called.set(true);
            }
        });
        
        manager.addDefaultRejectingCheck(new MockCheck(true, true));
        
        assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("a", "b", "c"), new SubmissionBuilder("s").build()));
        
        assertTrue(called.get());
    }
    
    @Test
    public void assignmentNotFoundCheckNotAdded() {
        AtomicBoolean submissionStored = new AtomicBoolean(false);
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage() {
            @Override
            public void submitNewVersion(SubmissionTarget target, Submission submission)
                    throws NoSuchTargetException, StorageException {
                submissionStored.set(true);
            }
        }, new EmptyStuMgmtView() {
            @Override
            public void fullReload() throws StuMgmtLoadingException {
                Course c = createCourse("c");
                Assignment a = createAssignment(c, "a", "a", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                a.setCheckConfigurationString(createJsonString(
                        new MockCheck(true, false, new ResultMessage("test", MessageType.ERROR, "mock"))
                ));
            }
        });
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "other", "g"), new SubmissionBuilder("s").build()));
        
        assertAll(
            () -> assertTrue(result.getAccepted()),
            () -> assertEquals(Collections.emptyList(), result.getMessages()),
            () -> assertTrue(submissionStored.get())
        );
    }
    
    @Test
    public void assignmentNotFoundDefaultRejectingStillRejecting() {
        AtomicBoolean submissionStored = new AtomicBoolean(false);
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage() {
            @Override
            public void submitNewVersion(SubmissionTarget target, Submission submission)
                    throws NoSuchTargetException, StorageException {
                submissionStored.set(true);
            }
        }, new EmptyStuMgmtView() {
            @Override
            public void fullReload() throws StuMgmtLoadingException {
                Course c = createCourse("c");
                createAssignment(c, "a", "a", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
            }
        });
        
        manager.addDefaultRejectingCheck(new MockCheck(true, false, new ResultMessage("test", MessageType.ERROR, "mock")));
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "other", "g"), new SubmissionBuilder("s").build()));
        
        assertAll(
            () -> assertFalse(result.getAccepted()),
            () -> assertEquals(
                    Arrays.asList(new CheckMessageDto("test", MessageType.ERROR, "mock")), result.getMessages()),
            () -> assertFalse(submissionStored.get())
        );
    }
    
    @Nested
    public class CreateCheck {
        
        @Test
        public void unknownCheckNameThrows() {
            SubmissionManager manager = new SubmissionManager(new EmptyStorage(), new EmptyStuMgmtView());
            
            CheckConfiguration config = new CheckConfiguration("doesnt exist", false);
            
            assertThrows(IllegalArgumentException.class, () -> manager.createCheck(config));
        }
        
        @Test
        public void encodingDefault() {
            SubmissionManager manager = new SubmissionManager(new EmptyStorage(), new EmptyStuMgmtView());
            
            CheckConfiguration config = new CheckConfiguration("encoding", false);
            
            Check check = manager.createCheck(config);
            
            assertAll(
                () -> assertInstanceOf(EncodingCheck.class, check),
                () -> assertEquals(StandardCharsets.UTF_8, ((EncodingCheck) check).getWantedCharset())
            );
        }
        
        @Test
        public void encodingUtf16() {
            SubmissionManager manager = new SubmissionManager(new EmptyStorage(), new EmptyStuMgmtView());
            
            CheckConfiguration config = new CheckConfiguration("encoding", false);
            config.setProperty("encoding", "utf-16");
            
            Check check = manager.createCheck(config);
            
            assertAll(
                () -> assertInstanceOf(EncodingCheck.class, check),
                () -> assertEquals(StandardCharsets.UTF_16, ((EncodingCheck) check).getWantedCharset())
            );
        }
        
        @Test
        public void encodingInvalidEncodingThrows() {
            SubmissionManager manager = new SubmissionManager(new EmptyStorage(), new EmptyStuMgmtView());
            
            CheckConfiguration config = new CheckConfiguration("encoding", false);
            config.setProperty("encoding", "invalid");
            
            assertThrows(IllegalArgumentException.class,() -> manager.createCheck(config));
        }
        
        @Test
        public void javacDefault() {
            SubmissionManager manager = new SubmissionManager(new EmptyStorage(), new EmptyStuMgmtView());
            
            CheckConfiguration config = new CheckConfiguration("javac", false);
            
            Check check = manager.createCheck(config);
            
            assertAll(
                () -> assertInstanceOf(JavacCheck.class, check),
                () -> assertEquals(11, ((JavacCheck) check).getJavaVersion())
            );
        }
        
        @Test
        public void javacVersion() {
            SubmissionManager manager = new SubmissionManager(new EmptyStorage(), new EmptyStuMgmtView());
            
            CheckConfiguration config = new CheckConfiguration("javac", false);
            config.setProperty("version", "17");
            
            Check check = manager.createCheck(config);
            
            assertAll(
                () -> assertInstanceOf(JavacCheck.class, check),
                () -> assertEquals(17, ((JavacCheck) check).getJavaVersion())
            );
        }
        
        @Test
        public void javacInvalidVersionThrows() {
            SubmissionManager manager = new SubmissionManager(new EmptyStorage(), new EmptyStuMgmtView());
            
            CheckConfiguration config = new CheckConfiguration("javac", false);
            config.setProperty("version", "invalid");
            
            assertThrows(IllegalArgumentException.class,() -> manager.createCheck(config));
        }
        
        @Test
        public void checkstyleNoRulesThrows() {
            SubmissionManager manager = new SubmissionManager(new EmptyStorage(), new EmptyStuMgmtView());
            
            CheckConfiguration config = new CheckConfiguration("checkstyle", false);
            
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,() -> manager.createCheck(config));
            assertEquals("Checkstyle check must have \"rules\" set", e.getMessage());
        }
        
        @Test
        public void checkstyle() {
            SubmissionManager manager = new SubmissionManager(new EmptyStorage(), new EmptyStuMgmtView());
            
            CheckConfiguration config = new CheckConfiguration("checkstyle", false);
            config.setProperty("rules", "basic.xml");
            
            Check check = manager.createCheck(config);
            
            assertAll(
                () -> assertInstanceOf(CheckstyleCheck.class, check),
                () -> assertEquals(Path.of("basic.xml"), ((CheckstyleCheck) check).getCheckstyleRules())
            );
        }
        
    }
    
    @Test
    public void submissionWithSameContentRejected() {
        AtomicBoolean submissionStored = new AtomicBoolean(false);
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage() {
                @Override
                public void submitNewVersion(SubmissionTarget target, Submission submission)
                        throws NoSuchTargetException, StorageException {
                    submissionStored.set(true);
                }
                @Override
                public List<Version> getVersions(SubmissionTarget target) throws NoSuchTargetException, StorageException {
                    return Arrays.asList(new Version("student3", Instant.now()));
                }
                @Override
                public Submission getSubmission(SubmissionTarget target, Version version)
                        throws NoSuchTargetException, StorageException {
                    
                    SubmissionBuilder sb = new SubmissionBuilder("student3");
                    sb.addUtf8File(Path.of("Main.java"), "some content");
                    sb.addUtf8File(Path.of("Util.java"), "some different content");
                    return sb.build();
                }
                
            }, new EmptyStuMgmtView() {
                @Override
                public void fullReload() throws StuMgmtLoadingException {
                    Course c = createCourse("c");
                    createAssignment(c, "a", "a", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                }
            });
        
        SubmissionBuilder sb = new SubmissionBuilder("student1");
        sb.addUtf8File(Path.of("Main.java"), "some content");
        sb.addUtf8File(Path.of("Util.java"), "some different content");
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "a", "g"), sb.build()));
        
        assertAll(
            () -> assertFalse(result.getAccepted()),
            () -> assertEquals(
                    Arrays.asList(new CheckMessageDto("submission", MessageType.WARNING,
                            "Submission is the same as the previous one")), result.getMessages()),
            () -> assertFalse(submissionStored.get())
        );
    }
    
    @Test
    public void submissionWithNoPreviousAccepted() {
        AtomicBoolean submissionStored = new AtomicBoolean(false);
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage() {
                @Override
                public void submitNewVersion(SubmissionTarget target, Submission submission)
                        throws NoSuchTargetException, StorageException {
                    submissionStored.set(true);
                }
            }, new EmptyStuMgmtView() {
                @Override
                public void fullReload() throws StuMgmtLoadingException {
                    Course c = createCourse("c");
                    createAssignment(c, "a", "a", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                }
            });
        
        SubmissionBuilder sb = new SubmissionBuilder("student1");
        sb.addUtf8File(Path.of("Main.java"), "some content");
        sb.addUtf8File(Path.of("Util.java"), "some different content");
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "a", "g"), sb.build()));
        
        assertAll(
            () -> assertTrue(result.getAccepted()),
            () -> assertEquals(Collections.emptyList(), result.getMessages()),
            () -> assertTrue(submissionStored.get())
        );
    }
    
    @Test
    public void submissionWithDifferentFilesAccepted() {
        AtomicBoolean submissionStored = new AtomicBoolean(false);
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage() {
                @Override
                public void submitNewVersion(SubmissionTarget target, Submission submission)
                        throws NoSuchTargetException, StorageException {
                    submissionStored.set(true);
                }
                @Override
                public List<Version> getVersions(SubmissionTarget target) throws NoSuchTargetException, StorageException {
                    return Arrays.asList(new Version("student3", Instant.now()));
                }
                @Override
                public Submission getSubmission(SubmissionTarget target, Version version)
                        throws NoSuchTargetException, StorageException {
                    
                    SubmissionBuilder sb = new SubmissionBuilder("student3");
                    sb.addUtf8File(Path.of("Main.java"), "some content");
                    sb.addUtf8File(Path.of("Util.java"), "some different content");
                    return sb.build();
                }
            }, new EmptyStuMgmtView() {
                @Override
                public void fullReload() throws StuMgmtLoadingException {
                    Course c = createCourse("c");
                    createAssignment(c, "a", "a", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                }
            });
        
        SubmissionBuilder sb = new SubmissionBuilder("student1");
        sb.addUtf8File(Path.of("Main2.java"), "some content");
        sb.addUtf8File(Path.of("Util.java"), "some different content");
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "a", "g"), sb.build()));
        
        assertAll(
            () -> assertTrue(result.getAccepted()),
            () -> assertEquals(Collections.emptyList(), result.getMessages()),
            () -> assertTrue(submissionStored.get())
        );
    }
    
    @Test
    public void submissionWithDifferentFileContentAccepted() {
        AtomicBoolean submissionStored = new AtomicBoolean(false);
        SubmissionManager manager = new TestSubmissionManager(new EmptyStorage() {
                @Override
                public void submitNewVersion(SubmissionTarget target, Submission submission)
                        throws NoSuchTargetException, StorageException {
                    submissionStored.set(true);
                }
                @Override
                public List<Version> getVersions(SubmissionTarget target) throws NoSuchTargetException, StorageException {
                    return Arrays.asList(new Version("student3", Instant.now()));
                }
                @Override
                public Submission getSubmission(SubmissionTarget target, Version version)
                        throws NoSuchTargetException, StorageException {
                    
                    SubmissionBuilder sb = new SubmissionBuilder("student3");
                    sb.addUtf8File(Path.of("Main.java"), "some content");
                    sb.addUtf8File(Path.of("Util.java"), "some different content");
                    return sb.build();
                }
            }, new EmptyStuMgmtView() {
                @Override
                public void fullReload() throws StuMgmtLoadingException {
                    Course c = createCourse("c");
                    createAssignment(c, "a", "a", StateEnum.IN_PROGRESS, CollaborationEnum.GROUP);
                }
            });
        
        SubmissionBuilder sb = new SubmissionBuilder("student1");
        sb.addUtf8File(Path.of("Main.java"), "different");
        sb.addUtf8File(Path.of("Util.java"), "some different content");
        
        SubmissionResultDto result = assertDoesNotThrow(() -> manager.submit(
                new SubmissionTarget("c", "a", "g"), sb.build()));
        
        assertAll(
            () -> assertTrue(result.getAccepted()),
            () -> assertEquals(Collections.emptyList(), result.getMessages()),
            () -> assertTrue(submissionStored.get())
        );
    }
    
    
}
