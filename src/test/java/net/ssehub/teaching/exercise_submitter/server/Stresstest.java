package net.ssehub.teaching.exercise_submitter.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.CollaborationEnum;
import net.ssehub.studentmgmt.backend_api.model.AssignmentDto.StateEnum;
import net.ssehub.studentmgmt.backend_api.model.ParticipantDto.RoleEnum;
import net.ssehub.studentmgmt.docker.StuMgmtDocker;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.AssignmentState;
import net.ssehub.studentmgmt.docker.StuMgmtDocker.Collaboration;
import net.ssehub.teaching.exercise_submitter.server.auth.AuthManager;
import net.ssehub.teaching.exercise_submitter.server.auth.PermissiveAuthManager;
import net.ssehub.teaching.exercise_submitter.server.logging.LoggingSetup;
import net.ssehub.teaching.exercise_submitter.server.rest.ExerciseSubmitterServer;
import net.ssehub.teaching.exercise_submitter.server.rest.dto.FileDto;
import net.ssehub.teaching.exercise_submitter.server.rest.routes.AbstractRestTest;
import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.filesystem.FilesystemStorage;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Assignment;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Course;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.EmptyStuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtLoadingException;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.submission.SubmissionManager;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.FileSizeCheck;

public class Stresstest {

    private static final Logger LOGGER = Logger.getLogger(Stresstest.class.getName());
    
    private AtomicInteger requestCounter = new AtomicInteger();
    
    private WebTarget target;
    
    private String courseId;
    
    private List<String> assignments;
    
    private List<String> groups;
    
    private List<String> tokens;
    
    private Deque<Integer> requestRuntimes = new ConcurrentLinkedDeque<>();
    
    private Stresstest(String baseUrl, String courseId, List<String> assignmentNames, List<String> groupNames, List<String> tokens) {
        Client client = ClientBuilder.newClient();
        target = client.target(baseUrl);
        
        this.courseId = courseId;
        this.assignments = assignmentNames;
        this.groups = groupNames;
        this.tokens = tokens;
    }
    
    public static Stresstest createLocalServerWithDummyStuMgmt(Path storageDirectory)  throws IOException, StorageException {
        List<String> assignments = new ArrayList<>(1000);
        
        AuthManager authManager = new PermissiveAuthManager();
        ISubmissionStorage storage = new FilesystemStorage(storageDirectory);
        StuMgmtView stuMgmtView = new EmptyStuMgmtView() {
            @Override
            public void fullReload() throws StuMgmtLoadingException {
                Course course = createCourse("course-wise2021");
                createParticipant(course, "user_1", "user_1", RoleEnum.STUDENT);
                createParticipant(course, "user_2", "user_2", RoleEnum.STUDENT);
                createParticipant(course, "user_3", "user_3", RoleEnum.STUDENT);
                
                for (int i = 0; i < 1000; i++) {
                    String name = String.format("assignment-%03d", i);
                    assignments.add(name);
                    Assignment a = createAssignment(course, name, name, StateEnum.IN_PROGRESS, CollaborationEnum.SINGLE);
                    a.setCheckConfigurationString("[{\"check\":\"encoding\",\"rejecting\":true}"
                            + ",{\"check\":\"javac\"},{\"check\":\"checkstyle\",\"rules\":\"src/test/resources/ScenarioIT/javaBeginners_checks.xml\"}]");
                }
            }
        };
        try {
            stuMgmtView.fullReload();
        } catch (StuMgmtLoadingException e) {
            e.printStackTrace();
        }
        storage.createOrUpdateAssignmentsFromView(stuMgmtView);
        SubmissionManager submissionManager = new SubmissionManager(storage, stuMgmtView);
        FileSizeCheck fileSizeCheck = new FileSizeCheck();
        fileSizeCheck.setMaxFileSize(1024 * 1024); // 1 MiB
        fileSizeCheck.setMaxSubmissionSize(1024 * 1024); // 1 MiB
        submissionManager.addDefaultRejectingCheck(fileSizeCheck);
        
        int port = AbstractRestTest.generateRandomPort();
        
        ExerciseSubmitterServer server = new ExerciseSubmitterServer();
        server.setPort(port);
        server.setAuthManager(authManager);
        server.setStorage(storage);
        server.setStuMgmtView(stuMgmtView);
        server.setSubmissionManager(submissionManager);
        
        LOGGER.config("Starting HTTP server on port " + port);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down HTTP server");
            server.stop();
        }));
        
        return new Stresstest("http://localhost:" + port + "/", "course-wise2021", assignments,
                Arrays.asList("user_1", "user_2", "user_3"), Arrays.asList("1", "2", "3"));
    }
    
    public static Stresstest createStresstestForDockerInstance(StuMgmtDocker docker, String courseId,
            List<String> assignmentNames, List<String> groupNames, List<String> usernames) {
        
        List<String> tokens = new ArrayList<>(usernames.size());
        for (String username : usernames) {
            tokens.add(docker.getAuthToken(username));
        }
        
        return new Stresstest(docker.getExerciseSubmitterServerUrl(), courseId, assignmentNames, groupNames, tokens);
    }
    
    private enum RequestType {
        
        SUBMISSION(201, "POST", "/submission/%1$s/%2$s/%3$s", true) {
            @Override
            public Entity<?> getEntity(int requestId) {
                FileDto main = new FileDto("Main.java", "public class Main {\n"
                        + "    public static void main(String[] args) {\n"
                        + "        System.out.println(\"Hello Request " + requestId + "\");\n"
                        + "    }\n"
                        + "}\n");
                
                return Entity.entity(Arrays.asList(main), MediaType.APPLICATION_JSON);
            }
        },
        
        COMPILATION_ERROR(201, "POST", "/submission/%1$s/%2$s/%3$s", true) {
            @Override
            public Entity<?> getEntity(int requestId) {
                FileDto main = new FileDto("Main.java", "public class Main {\n"
                        + "    public static void main(String[] args) {\n"
                        + "        System.println(\"Hello Request " + requestId + "\");\n"
                        + "    }\n"
                        + "}\n");
                
                return Entity.entity(Arrays.asList(main), MediaType.APPLICATION_JSON);
            }
        },
        
        CHECKSTYLE_ERROR(201, "POST", "/submission/%1$s/%2$s/%3$s", true) {
            @Override
            public Entity<?> getEntity(int requestId) {
                FileDto main = new FileDto("Main.java", "public class Main {\n"
                        + "public static void main(String[] args) {\n"
                        + "System.out.println(\"Hello Request " + requestId + "\");\n"
                        + "}\n"
                        + "}\n");
                
                return Entity.entity(Arrays.asList(main), MediaType.APPLICATION_JSON);
            }
        },
        
        INVALID_ENCODING(200, "POST", "/submission/%1$s/%2$s/%3$s", true) {
            @Override
            public Entity<?> getEntity(int requestId) {
                FileDto main = new FileDto("Main.java", ("public class Main {\n"
                        + "    public static void main(String[] args) {\n"
                        + "        System.out.println(\"Hellö Request " + requestId + "\");\n"
                        + "    }\n"
                        + "}\n").getBytes(StandardCharsets.ISO_8859_1));
                
                return Entity.entity(Arrays.asList(main), MediaType.APPLICATION_JSON);
            }
        },
        
        NO_AUTHENTICATION(403, "POST", "/submission/%1$s/%2$s/%3$s", false) {
            @Override
            public Entity<?> getEntity(int requestId) {
                FileDto main = new FileDto("Main.java", "public class Main {\n"
                        + "    public static void main(String[] args) {\n"
                        + "        System.out.println(\"Hello Request " + requestId + "\");\n"
                        + "    }\n"
                        + "}\n");
                
                return Entity.entity(Arrays.asList(main), MediaType.APPLICATION_JSON);
            }
        },
        
        LIST_VERSIONS(200, "GET", "/submission/%1$s/%2$s/%3$s/versions", true),
        
        REPLAY_LATEST(200, "GET", "/submission/%1$s/%2$s/%3$s/latest", true) {
            @Override
            public boolean checkStatusCode(int statusCode) {
                return statusCode == 200 || statusCode == 404;
            }
        },
        
        GET_INVALID_VERSION(404, "GET", "/submission/%1$s/%2$s/%3$s/123456", true);
        
        private int expectedStatusCode;
        
        private String httpMethod;
        
        private String pathSchema;
        
        private boolean withAuth;
        
        private RequestType(int expectedStatusCode, String httpMethod, String pathSchema, boolean withAuth) {
            this.expectedStatusCode = expectedStatusCode;
            this.httpMethod = httpMethod;
            this.pathSchema = pathSchema;
            this.withAuth = withAuth;
        }
        
        public boolean checkStatusCode(int statusCode) {
            return statusCode == expectedStatusCode;
        }
        
        public String getHttpMethod() {
            return httpMethod;
        }
        
        public String getPathSchema() {
            return pathSchema;
        }
        
        public boolean getWithAuth() {
            return withAuth;
        }
        
        public Entity<?> getEntity(int requestId) {
            return null;
        }
        
        public static RequestType getRandomRequestType() {
            return values()[(int) (Math.random() * values().length)];
        }
    }
    
    private void doRequest(RequestType requestType) {
        int requestNumber = requestCounter.getAndIncrement();
        int assignmentIndex = (int) (Math.random() * assignments.size());
        int groupIndex = (int) (Math.random() * groups.size());
//        int assignmentIndex = (requestNumber / groups.size()) % assignments.size();
//        int groupIndex = requestNumber % groups.size();
        
        
        Builder requestBuilder = target.path(
                String.format(requestType.getPathSchema(), courseId, assignments.get(assignmentIndex), groups.get(groupIndex)))
                .request();
        
        if (requestType.getWithAuth()) {
            requestBuilder.header("Authorization", "Bearer " + tokens.get(groupIndex));
        }
        
        Invocation invocation = requestBuilder.build(requestType.getHttpMethod(), requestType.getEntity(requestNumber));
        
        long t0 = System.currentTimeMillis();
        Response response = invocation.invoke();
        long t1 = System.currentTimeMillis();
        requestRuntimes.add((int) (t1 - t0));
        
        if (!requestType.checkStatusCode(response.getStatus())) {
            LOGGER.warning(() -> "Got invalid response code " + response.getStatus() + " for request type " + requestType.name()
                    + "\nreason phrase: " + response.getStatusInfo().getReasonPhrase()
                    + "\nresponse body: " + response.readEntity(String.class));
        }
    }
    
    public void stresstest(int numThreads, int numRequestsPerThread) {
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int request = 0; request < numRequestsPerThread; request++) {
                    doRequest(RequestType.getRandomRequestType());
                }
            });
        }
        
        Arrays.stream(threads).forEach(Thread::start);
        Arrays.stream(threads).forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        
        IntSummaryStatistics runtimeStats = requestRuntimes.stream().mapToInt(Integer::intValue).summaryStatistics();
        LOGGER.info("Request runtime stats: count=" + runtimeStats.getCount() + ", average=" + runtimeStats.getAverage()
                + "ms, min=" + runtimeStats.getMin() + "ms, max=" + runtimeStats.getMax() + "ms");
        LOGGER.info("Request runtimes: " + requestRuntimes);
    }
    
    @SuppressWarnings("unused")
    private static void local(String path) {
        try {
            Path storage = Path.of(path);
            if (!Files.isDirectory(storage)) {
                Files.createDirectories(storage);
            }
            Stresstest stresstest = Stresstest.createLocalServerWithDummyStuMgmt(storage);
            stresstest.stresstest(4, 100);
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }
    
    private static void docker() {
        final int numStudents = 50;
        final int numAssignments = 14;
        
        try (StuMgmtDocker docker = new StuMgmtDocker()) {
            docker.createUser("teacher", "123456");
            String course = docker.createCourse("java", "wise2122", "Some Course", "teacher");
            
            for (int i = 1; i <= numStudents; i++) {
                String name = String.format("student%03d", i);
                docker.createUser(name, "abcdefgh");
                docker.enrollStudent(course, name);
            }
            
            List<String> groupNames = new ArrayList<>(numStudents / 2);
            List<String> usernames = new ArrayList<>(numStudents / 2);
            for (int i = 1; i <= numStudents / 2; i++) {
                String groupMember1 = String.format("student%03d", i * 2 - 1);
                String groupMember2 = String.format("student%03d", i * 2);
                String name = String.format("Group%03d", i);
                groupNames.add(name);
                usernames.add(groupMember1);
                docker.createGroup(course, name, groupMember1, groupMember2);
            }
            
            docker.enableExerciseSubmissionServer(course);
            List<String> assignmentNames = new ArrayList<>(numAssignments);
            for (int i = 1; i <= numAssignments; i++) {
                String name = String.format("Homework%02d", i);
                assignmentNames.add(name);
                String assignment = docker.createAssignment(course, name, AssignmentState.SUBMISSION, Collaboration.GROUP);
                docker.setAssignmentToolConfigString(course, assignment, "exercise-submitter-checks",
                        "[{\"check\":\"encoding\",\"rejecting\":true},{\"check\":\"javac\"},{\"check\":\"checkstyle\",\"rules\":\"checkstyle.xml\"}]");
            }
            
            LOGGER.info("Docker set up, waiting a few seconds for things to settle down...");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
            
            Stresstest stresstest = createStresstestForDockerInstance(docker, course, assignmentNames, groupNames, usernames);
            stresstest.stresstest(1, 300);
            
            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
    }
    
    public static void main(String[] args) throws IOException, StorageException {
        LoggingSetup.init();
        LoggingSetup.setLevel("INFO");
        
//        local(args[0]);
        docker();
    }
    
}
