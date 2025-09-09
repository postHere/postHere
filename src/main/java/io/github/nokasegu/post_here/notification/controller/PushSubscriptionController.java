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
