package io.github.nokasegu.post_here.notification.service;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * WebPushProperties
 * <p>
 * 역할(정확 명칭)
 * - application.yml / application-secret.yml 등에 정의된 `webpush.*` 설정을 바인딩하는 구성 클래스.
 * · subject   : VAPID 구독자 식별(일반적으로 "mailto:admin@example.com" 형식 권장)
 * · publicKey : VAPID 공개키 (브라우저 구독 생성 시 클라이언트에도 노출 가능)
 * · privateKey: VAPID 비밀키 (서버 전용 · 유출 금지)
 * <p>
 * 사용처(정확 명칭)
 * - 서버: WebPushService.buildClient()에서 PushService 초기화 시 subject/public/private 키 주입
 * <p>
 * 보안/운영 메모
 * - privateKey는 소스 저장소에 두지 말고, 환경변수/비밀 설정 파일/시크릿 매니저 등을 통해 주입하세요.
 * - 다중 환경(개발/스테이징/운영)에서 서로 다른 VAPID 키쌍을 사용하도록 프로필 분리 권장.
 * - 값 검증이 필요하면 @Validated와 @NotBlank 등을 추가해 런타임 바인딩 실패를 조기 감지할 수 있습니다(현 코드에는 미적용: 문서 목적).
 * <p>
 * 예시(application-secret.yml)
 * webpush:
 * subject: "mailto:admin@example.com"
 * publicKey: "YOUR_VAPID_PUBLIC_KEY"
 * privateKey: "YOUR_VAPID_PRIVATE_KEY"
 */
@Getter
@Component
@ConfigurationProperties(prefix = "webpush")
public class WebPushProperties {
    private String subject;
    private String publicKey;
    private String privateKey;

    // setter는 @ConfigurationProperties 바인딩을 위해 public 필요
    public void setSubject(String v) {
        this.subject = v;
    }

    public void setPublicKey(String v) {
        this.publicKey = v;
    }

    public void setPrivateKey(String v) {
        this.privateKey = v;
    }
}
