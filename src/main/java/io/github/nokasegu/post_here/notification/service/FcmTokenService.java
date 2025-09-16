package io.github.nokasegu.post_here.notification.service;

import io.github.nokasegu.post_here.notification.domain.FcmTokenEntity;
import io.github.nokasegu.post_here.notification.dto.FcmTokenRequestDto;
import io.github.nokasegu.post_here.notification.repository.FcmTokenRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FcmTokenService
 * - FCM registration token 저장/갱신 (멱등)
 */
@Service
@RequiredArgsConstructor
@Transactional // 클래스 단위 (컨벤션 준수)
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserInfoRepository userInfoRepository;

    public void saveOrUpdate(String email, FcmTokenRequestDto req) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("인증 필요");
        }
        if (req == null || req.getToken() == null || req.getToken().isBlank()) {
            throw new IllegalArgumentException("token 필요");
        }

        UserInfoEntity user = userInfoRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음: " + email));

        // token 기준 멱등 upsert
        FcmTokenEntity entity = fcmTokenRepository.findByToken(req.getToken())
                .map(e -> {
                    e.setUser(user);
                    e.setPlatform(req.getPlatform());
                    e.setApp(req.getApp());
                    return e;
                })
                .orElseGet(() -> req.convertToEntity(user));

        fcmTokenRepository.save(entity);
    }
}
