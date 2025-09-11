// src/main/java/io/github/nokasegu/post_here/notification/controller/PushSubscriptionController.java
package io.github.nokasegu.post_here.notification.controller;

import io.github.nokasegu.post_here.notification.domain.PushSubscriptionEntity;
import io.github.nokasegu.post_here.notification.dto.PushSubscribeRequestDto;
import io.github.nokasegu.post_here.notification.repository.PushSubscriptionRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

/**
 * [역할]
 * - 브라우저에서 생성한 PushSubscription 객체를 서버에 저장하는 컨트롤러.
 * - 구독 정보는 endpoint를 고유키로 하여 upsert 방식으로 관리한다.
 * <p>
 * [엔드포인트]
 * - POST /push/subscribe
 * → body: { endpoint, keys:{ p256dh, auth } }
 * <p>
 * [동작 흐름]
 * 1) Principal에서 사용자 식별(email 기반)
 * 2) endpoint 기준으로 기존 구독 검색
 * 3) 없으면 새로운 PushSubscriptionEntity 생성 후 저장
 * <p>
 * [보안 메모]
 * - principal이 null이거나 사용자 조회 실패 시 401/404 반환
 * - 구독 저장 시 endpoint는 고유키로 중복 방지
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/push")
public class PushSubscriptionController {
    private final PushSubscriptionRepository pushRepo;
    private final UserInfoRepository userRepo;

    private UserInfoEntity resolveUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 필요");
        }
        return userRepo.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 없음"));
    }

    @PostMapping("/subscribe")
    public void subscribe(Principal principal, @RequestBody PushSubscribeRequestDto body) {
        UserInfoEntity me = resolveUser(principal);
        pushRepo.findByEndpoint(body.getEndpoint())
                .orElseGet(() -> pushRepo.save(
                        PushSubscriptionEntity.builder()
                                .user(me)
                                .endpoint(body.getEndpoint())
                                .p256dh(body.getKeys().getP256dh())
                                .auth(body.getKeys().getAuth())
                                .build()
                ));
    }
}
