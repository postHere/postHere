package io.github.nokasegu.post_here.notification.controller;

import io.github.nokasegu.post_here.notification.dto.MarkReadRequestDto;
import io.github.nokasegu.post_here.notification.dto.NotificationListRequestDto;
import io.github.nokasegu.post_here.notification.dto.NotificationListResponseDto;
import io.github.nokasegu.post_here.notification.service.NotificationService;
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
@RequestMapping("/notification")
public class NotificationApiController {

    private final NotificationService notificationService;
    private final UserInfoRepository userRepo;

    private Long resolveUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 필요");
        }
        UserInfoEntity u = userRepo.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 없음"));
        return u.getId();
    }

    // ✅ Body Only: page,size도 바디로 받음
    @PostMapping("/list")
    public NotificationListResponseDto list(Principal principal,
                                            @RequestBody(required = false) NotificationListRequestDto body) {
        Long me = resolveUserId(principal);
        int page = body == null ? 0 : body.safePage();
        int size = body == null ? 20 : body.safeSize();
        return notificationService.list(me, page, size);
    }

    @PostMapping("/read")
    public long read(Principal principal, @RequestBody MarkReadRequestDto body) {
        Long me = resolveUserId(principal);
        return notificationService.markRead(me, body.getNotificationIds());
    }

    @PostMapping("/read-all")
    public long readAll(Principal principal) {
        Long me = resolveUserId(principal);
        return notificationService.markAllRead(me);
    }

    @PostMapping("/unread-count")
    public long unreadCount(Principal principal) {
        Long me = resolveUserId(principal);
        return notificationService.unreadCount(me);
    }
}
