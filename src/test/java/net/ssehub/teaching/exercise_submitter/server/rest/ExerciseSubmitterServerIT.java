package net.ssehub.teaching.exercise_submitter.server.rest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.junit.jupiter.api.Test;

import net.ssehub.teaching.exercise_submitter.server.auth.PermissiveAuthManager;
import net.ssehub.teaching.exercise_submitter.server.rest.routes.AbstractRestTest;
import net.ssehub.teaching.exercise_submitter.server.storage.EmptyStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.EmptyStuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.submission.NoChecksSubmissionManager;

public class ExerciseSubmitterServerIT {
    
    private static final String KEYSTORE_PASSWORD = "123456";

    @Test
    public void startServerReachable() throws IOException {
        int port = AbstractRestTest.generateRandomPort();
        
        ExerciseSubmitterServer server = new ExerciseSubmitterServer();
        server.setPort(port);
        ISubmissionStorage storage = new EmptyStorage();
        server.setSubmissionManager(new NoChecksSubmissionManager(storage));
        server.setStorage(storage);
        server.setAuthManager(new PermissiveAuthManager());
        server.setStuMgmtView(new EmptyStuMgmtView());

        try {
            assertDoesNotThrow(() -> server.start());
            
            HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + "/heartbeat").openConnection();
            int code = connection.getResponseCode();
            assertEquals(200, code);
            
        } finally {
            try {
                server.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @Test
    public void startTlsServerReachable() throws IOException, GeneralSecurityException {
        Path keystore = null;
        ExerciseSubmitterServer server = null;
        
        try {
            keystore = createTemporaryKeystore();
        
            int port = AbstractRestTest.generateRandomPort();
            
            server = new ExerciseSubmitterServer();
            server.setPort(port);
            ISubmissionStorage storage = new EmptyStorage();
            server.setSubmissionManager(new NoChecksSubmissionManager(storage));
            server.setStorage(storage);
            server.setAuthManager(new PermissiveAuthManager());
            server.setStuMgmtView(new EmptyStuMgmtView());
            server.setTlsKeystore(keystore, KEYSTORE_PASSWORD);
            server.start();
            
            HttpsURLConnection connection = (HttpsURLConnection) new URL("https://localhost:" + port + "/heartbeat").openConnection();
            connection.setSSLSocketFactory(createSocketFactoryTrusingCert(keystore));
            int code = connection.getResponseCode();
            assertEquals(200, code);
            
        } finally {
            if (keystore != null) {
                try {
                    Files.delete(keystore);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (server != null) {
                try {
                    server.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private static Path createTemporaryKeystore() throws IOException {
        Path keystore = Files.createTempFile("ExerciseSubmitterServerIT.keystore.", ".jks");
        Files.delete(keystore);
        
        ProcessBuilder pb = new ProcessBuilder("keytool",
                "-genkey",
                "-keyalg", "RSA",
                "-keystore", keystore.toAbsolutePath().toString(),
                "-keysize", "2048",
                "-storepass", KEYSTORE_PASSWORD,
                "-dname", "CN=localhost");
        
        pb.inheritIO();
        Process p = pb.start();
        
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        return keystore;
    }
    
    private static SSLSocketFactory createSocketFactoryTrusingCert(Path keystore) throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(Files.newInputStream(keystore), KEYSTORE_PASSWORD.toCharArray());
        
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        
        SSLContext tlsContext = SSLContext.getInstance("TLS");
        tlsContext.init(null, trustManagerFactory.getTrustManagers(), null);
        
        return tlsContext.getSocketFactory();
    }
}
