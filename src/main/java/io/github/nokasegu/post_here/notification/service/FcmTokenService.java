package io.github.nokasegu.post_here.notification.service;

import io.github.nokasegu.post_here.notification.domain.FcmTokenEntity;
import io.github.nokasegu.post_here.notification.dto.FcmTokenRequestDto;
import io.github.nokasegu.post_here.notification.repository.FcmTokenRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

/**
 * FcmTokenService
 * <p>
 * 역할
 * - 로그인 사용자 기준으로 FCM 토큰 upsert
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserInfoRepository userInfoRepository;

    public void upsert(Principal principal, FcmTokenRequestDto req) {
        if (principal == null || principal.getName() == null) {
            throw new IllegalStateException("인증 필요");
        }

        // principal.name = email (현재 보안 설정과 일관)
        UserInfoEntity user = userInfoRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        fcmTokenRepository.findByToken(req.getToken())
                .orElseGet(() -> fcmTokenRepository.save(
                        FcmTokenEntity.builder()
                                .user(user)
                                .token(req.getToken())
                                .platform(req.getPlatform())
                                .appName(req.getApp())
                                .build()
                ));
    }
}
