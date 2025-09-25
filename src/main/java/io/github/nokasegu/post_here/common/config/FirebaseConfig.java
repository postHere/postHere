// src/main/java/io/github/nokasegu/post_here/common/config/FirebaseConfig.java
package io.github.nokasegu.post_here.common.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class FirebaseConfig {

    private final ResourceLoader resourceLoader;

    // [변경 이유]
    // - yml의 키와 정확히 일치하도록 @Value 사용
    //   application-secret.yml 예시:
    //   firebase:
    //     credentials: "file:C:/Users/user/secrets/firebase-service-account.json"
    //     project-id: "posthere-52fe8"
    //
    // - 'project-id'는 하이픈 표기로 읽습니다(여기서도 동일 키 사용).
    @Value("${firebase.credentials:#{null}}")
    private String firebaseCredentialsPath;

    @Value("${firebase.project-id:#{null}}")
    private String firebaseProjectId;

    /**
     * FirebaseApp 생성
     * <p>
     * [핵심 변경]
     * 1) credentials 경로가 주어졌으면 해당 JSON을 읽어 GoogleCredentials 생성
     * 2) projectId는 우선순위: (프로퍼티) firebase.project-id → (JSON) service account의 project_id
     * 3) projectId가 최종적으로 비어있으면 명시적 예외 + 가이드
     * 4) 이미 FirebaseApp이 초기화돼 있으면 재사용
     */
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        GoogleCredentials credentials;
        String projectIdResolved = null;
        String projectIdSource = "unset";
        String credsSource = "unset";

        // 1) Credentials 로딩
        if (StringUtils.hasText(firebaseCredentialsPath)) {
            Resource resource = resourceLoader.getResource(firebaseCredentialsPath);
            if (!resource.exists()) {
                throw new IllegalStateException("[Firebase] Service account file not found: " + firebaseCredentialsPath);
            }

            byte[] jsonBytes;
            try (InputStream in = resource.getInputStream()) {
                jsonBytes = in.readAllBytes();
            }

            credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(jsonBytes));
            credsSource = resource.getDescription();

            // 2) service account JSON에서 project_id를 직접 파싱(프로퍼티가 비어있을 때만 사용)
            if (!StringUtils.hasText(firebaseProjectId)) {
                try {
                    Map<String, Object> json = new ObjectMapper().readValue(
                            jsonBytes, new TypeReference<Map<String, Object>>() {
                            });
                    Object pid = json.get("project_id");
                    if (pid != null && StringUtils.hasText(pid.toString())) {
                        projectIdResolved = pid.toString().trim();
                        projectIdSource = "service-account";
                    }
                } catch (Exception e) {
                    log.warn("[Firebase] Unable to parse 'project_id' from service account JSON: {}", e.toString());
                }
            }
        } else {
            // credentials 경로가 없으면 ADC 시도(개발용/CI 등)
            credentials = GoogleCredentials.getApplicationDefault();
            credsSource = "ApplicationDefaultCredentials";
        }

        // 3) 프로퍼티가 있으면 최우선
        if (StringUtils.hasText(firebaseProjectId)) {
            projectIdResolved = firebaseProjectId.trim();
            projectIdSource = "property(firebase.project-id)";
        }

        if (!StringUtils.hasText(projectIdResolved)) {
            // [중요] v1 API는 projectId 필요 → 명확한 가이드 메시지
            throw new IllegalStateException("""
                    [Firebase] Project ID is missing.
                    - Set 'firebase.project-id' in application-secret.yml (and import the file),
                      or use a service account JSON that contains 'project_id'.""");
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectIdResolved)
                .build();

        // 이미 초기화된 인스턴스가 있으면 재사용
        FirebaseApp app;
        try {
            app = FirebaseApp.initializeApp(options);
            log.info("[Firebase] Admin SDK initialized. appName={}, projectId={}, creds={}",
                    app.getName(), projectIdResolved, credsSource);
            log.info("[Firebase] projectId source={}, credentials source={}", projectIdSource, credsSource);
        } catch (IllegalStateException already) {
            app = FirebaseApp.getInstance();
            log.info("[Firebase] Reusing existing FirebaseApp. appName={}, projectId={}",
                    app.getName(), app.getOptions().getProjectId());
        }

        return app;
    }

    /**
     * FirebaseMessaging 생성
     * <p>
     * [추가 로그]
     * - init 완료 로그를 명확히 찍어 디버깅 편의↑
     */
    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp app) {
        FirebaseMessaging messaging = FirebaseMessaging.getInstance(app);
        log.info("[Firebase] FirebaseMessaging ready. projectId={}", app.getOptions().getProjectId());
        return messaging;
    }
}
