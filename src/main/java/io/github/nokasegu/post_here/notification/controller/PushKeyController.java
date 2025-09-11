// src/main/java/io/github/nokasegu/post_here/notification/controller/PushKeyController.java
package io.github.nokasegu.post_here.notification.controller;

import io.github.nokasegu.post_here.notification.service.WebPushProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * [역할]
 * - 프론트엔드(main-nav.js, service-worker.js)에서 PushManager.subscribe() 호출 시
 * 필요한 VAPID 공개키를 내려주는 전용 컨트롤러.
 * <p>
 * [엔드포인트]
 * - GET /push/vapid-public-key
 * → 응답 JSON: { "publicKey": "<base64url string>" }
 * <p>
 * [보안 메모]
 * - VAPID 공개키는 노출되어도 안전하지만,
 * Private Key는 서버(WebPushProperties)에만 보관해야 한다.
 */
@RestController
@RequiredArgsConstructor
public class PushKeyController {
    private final WebPushProperties props;

    @GetMapping(value = "/push/vapid-public-key", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> key() {
        // 공개키를 단일 JSON 객체로 반환
        return Map.of("publicKey", props.getPublicKey());
    }
}
