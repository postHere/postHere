package io.github.nokasegu.post_here.notification.controller;

import io.github.nokasegu.post_here.notification.service.NotificationService;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 읽음 처리 단일 엔드포인트
 * - 규칙: /notification 뒤에 다른 경로 세그먼트 금지
 * - 전체 읽음: {} 또는 {"all": true}
 * - 선택 읽음: {"ids":[1,2,3]}
 */
@RestController
@RequiredArgsConstructor
public class NotificationReadController {

    private final NotificationService notificationService;
    private final UserInfoRepository userRepo;

    private Long resolveUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 필요");
        }
        // principal.getName()은 email
        UserInfoEntity byEmail = userRepo.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 없음"));
        return byEmail.getId();
    }

    @PostMapping(value = "/notification", consumes = "application/json", produces = "application/json")
    public Map<String, Integer> read(Principal principal, @RequestBody(required = false) ReadRequest body) {
        Long me = resolveUserId(principal);

        // ids가 있으면 선택 읽음, 아니면 전체 읽음
        int updated;
        if (body != null && body.getIds() != null && !body.getIds().isEmpty()) {
            updated = notificationService.markRead(me, body.getIds());
        } else {
            updated = notificationService.markAllRead(me);
        }
        return Collections.singletonMap("updated", updated);
    }

    @Data
    public static class ReadRequest {
        private Boolean all;
        private List<Long> ids;
    }
}
