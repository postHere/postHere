package io.github.nokasegu.post_here.notification.controller;

import io.github.nokasegu.post_here.notification.dto.MarkReadRequestDto;
import io.github.nokasegu.post_here.notification.dto.NotificationListRequestDto;
import io.github.nokasegu.post_here.notification.dto.NotificationListResponseDto;
import io.github.nokasegu.post_here.notification.service.NotificationService;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserInfoRepository userRepo;

    /**
     * 현재 인증 사용자 ID 해석
     * - principal.getName()이 이메일이면 email로 조회
     * - 숫자(문자열)라면 PK로도 시도
     */
    private Long resolveUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 필요");
        }
        String name = principal.getName();

        // 1) 이메일로 조회 시도
        UserInfoEntity byEmail = userRepo.findByEmail(name).orElse(null);
        if (byEmail != null) return byEmail.getId();

        // 2) 숫자 문자열이면 PK 조회 시도
        try {
            long asId = Long.parseLong(name);
            return userRepo.findById(asId)
                    .map(UserInfoEntity::getId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 없음"));
        } catch (NumberFormatException ignored) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 주체를 해석할 수 없음");
        }
    }

    // 목록 (body로 page/size 받는 현재 방식 유지)
    @PostMapping("/list")
    public NotificationListResponseDto list(Principal principal,
                                            @RequestBody(required = false) NotificationListRequestDto body) {
        Long me = resolveUserId(principal);
        int page = (body == null) ? 0 : body.safePage();
        int size = (body == null) ? 20 : body.safeSize();

        // [변경] 서비스가 "FOLLOW 최신 1건만 + NULL 연결 제외 + 오버페치 + hasNext 계산"을 수행하여 반환
        return notificationService.list(me, page, size);
    }

    // 선택 읽음
    @PostMapping("/read")
    public int read(Principal principal, @RequestBody MarkReadRequestDto body) {
        Long me = resolveUserId(principal);
        return notificationService.markRead(me, body.getNotificationIds());
    }

    // 전체 읽음
    @PostMapping("/read-all")
    public int readAll(Principal principal) {
        Long me = resolveUserId(principal);
        return notificationService.markAllRead(me);
    }

    // 미읽음 카운트
    @PostMapping("/unread-count")
    public long unreadCount(Principal principal) {
        Long me = resolveUserId(principal);
        return notificationService.unreadCount(me);
    }
}
