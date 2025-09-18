package io.github.nokasegu.post_here.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * FirebaseConfig
 * <p>
 * 역할
 * - 서버 기동 시 FirebaseApp을 1회 초기화하여 FCM HTTP v1 사용 가능하게 함.
 * <p>
 * 설정
 * - application-secret.yml
 * firebase:
 * credentials: "file:/var/config/firebase-service-account.json"
 * project-id:  "YOUR_FIREBASE_PROJECT_ID"
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials}")
    private String credentialsPath; // file:/... 경로 권장

    @Value("${firebase.project-id}")
    private String projectId;

    @PostConstruct
    public void init() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                log.info("FirebaseApp already initialized. Skipping.");
                return;
            }

            String path = credentialsPath;
            if (path.startsWith("file:")) {
                path = path.substring("file:".length());
            }

            try (FileInputStream serviceAccount = new FileInputStream(path)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setProjectId(projectId)
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("FirebaseApp initialized (projectId={})", projectId);
            }
        } catch (IOException e) {
            log.error("Failed to initialize FirebaseApp", e);
        }
    }
}
