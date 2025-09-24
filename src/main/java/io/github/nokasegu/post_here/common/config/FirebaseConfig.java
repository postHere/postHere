// src/main/java/io/github/nokasegu/post_here/common/config/FirebaseConfig.java
package io.github.nokasegu.post_here.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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

            // [수정] file:, classpath: 모두 지원
            InputStream serviceAccountStream;
            if (path.startsWith("file:")) {
                String real = path.substring("file:".length());
                serviceAccountStream = new FileInputStream(real);
            } else if (path.startsWith("classpath:")) {
                String cp = path.substring("classpath:".length());
                serviceAccountStream = new ClassPathResource(cp).getInputStream();
            } else {
                // 스킴이 없으면 파일 경로로 간주
                serviceAccountStream = new FileInputStream(path);
            }

            try (InputStream in = serviceAccountStream) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(in))
                        .setProjectId(projectId)
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("FirebaseApp initialized (projectId={})", projectId);
            }
        } catch (IOException e) {
            log.error("Failed to initialize FirebaseApp", e);
        }
    }

    // [추가] FcmSenderService에서 주입받을 FirebaseMessaging 빈 정의
    // - init()에서 FirebaseApp이 초기화되므로 여기서는 기본 인스턴스를 반환
    // - 만약 외부에서 먼저 초기화되었다면 기본 인스턴스를 재사용
    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (FirebaseApp.getApps().isEmpty()) {
            // [추가 설명] 이론상 @PostConstruct가 먼저 실행되지만, 혹시를 대비해 보호 로직
            init();
        }
        return FirebaseMessaging.getInstance();
    }
}
