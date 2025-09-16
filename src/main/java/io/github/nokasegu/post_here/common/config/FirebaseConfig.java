package io.github.nokasegu.post_here.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

/**
 * FirebaseConfig
 * - 서버 기동 시 FirebaseApp을 1회 초기화
 * - firebase.credentials / firebase.project-id 설정 사용
 */
@Configuration
@RequiredArgsConstructor
public class FirebaseConfig {

    private final ResourceLoader resourceLoader;

    @Value("${firebase.credentials}")
    private String credentialsLocation;

    @Value("${firebase.project-id}")
    private String projectId;

    @PostConstruct
    public void initFirebase() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) return;

        Resource resource = resourceLoader.getResource(credentialsLocation);
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(resource.getInputStream()))
                .setProjectId(projectId)
                .build();
        FirebaseApp.initializeApp(options);
    }
}
