// src/main/java/io/github/nokasegu/post_here/notification/service/NotificationService.java
package io.github.nokasegu.post_here.notification.service;

import io.github.nokasegu.post_here.follow.domain.FollowingEntity;
import io.github.nokasegu.post_here.follow.repository.FollowingRepository;
import io.github.nokasegu.post_here.forum.domain.ForumCommentEntity;
import io.github.nokasegu.post_here.forum.repository.ForumCommentRepository;
import io.github.nokasegu.post_here.notification.domain.NotificationCode;
import io.github.nokasegu.post_here.notification.domain.NotificationEntity;
import io.github.nokasegu.post_here.notification.dto.NotificationItemResponseDto;
import io.github.nokasegu.post_here.notification.dto.NotificationListResponseDto;
import io.github.nokasegu.post_here.notification.repository.NotificationRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserInfoRepository userInfoRepository;
    private final WebPushService webPushService;
    private final FcmSenderService fcmSenderService;

    // ===================== [추가] 안전 재조회용 리포지토리 =====================
    private final ForumCommentRepository forumCommentRepository;
    private final FollowingRepository followingRepository;
    // ======================================================================

    /**
     * AFTER_COMMIT 리스너에서 호출되는 알림 생성/전송 메서드.
     * 기존 트랜잭션이 이미 끝난 뒤 실행되므로 "새 트랜잭션"을 강제로 시작한다.
     * (transactionManager 이름은 JPA TM 빈 이름에 맞게 필요 시 변경)
     * <p>
     * [중요 변경]
     * - REQUIRES_NEW 진입 직후, 전달받은 FollowingEntity를 "ID 기반으로 재조회(fetch-join)" 하여
     * 지연로딩을 안전하게 처리한다.
     * - 전송 순서: FCM → Web Push (둘 다 시도)
     */
    @Transactional(transactionManager = "transactionManager", propagation = Propagation.REQUIRES_NEW)
    public NotificationEntity createFollowAndPush(FollowingEntity following) {
        Long fid = (following != null ? following.getId() : null);
        // [추가 로그]
        log.info("[NOTI] createFollowAndPush called. followingId={}", fid);

        if (fid == null) {
            log.warn("[NOTI] createFollowAndPush skipped: following id is null");
            return null;
        }
        FollowingEntity f = followingRepository.findByIdWithBothUsers(fid)
                .orElseThrow(() -> new IllegalArgumentException("Following not found: id=" + fid));

        NotificationEntity saved = notificationRepository.saveAndFlush(
                NotificationEntity.builder()
                        .notificationCode(NotificationCode.FOLLOW)
                        .following(f)                         // ← 다시 조회한 f 사용
                        .targetUser(f.getFollowed())
                        .checkStatus(false)
                        .build()
        );
        // [추가 로그]
        log.info("[NOTI] FOLLOW saved. notiId={} targetUserId={} actorId={}",
                saved.getId(),
                (f.getFollowed() != null ? f.getFollowed().getId() : null),
                (f.getFollower() != null ? f.getFollower().getId() : null));

        // 1) FCM (최우선)
        try {
            fcmSenderService.sendFollow(
                    f.getFollowed(),
                    f.getFollower().getNickname(),
                    f.getFollower().getProfilePhotoUrl(),
                    saved.getId()
            );
        } catch (Exception e) {
            log.warn("[NOTI] FCM send failed (FOLLOW) id={}, err={}", saved.getId(), e.toString());
        }

        // 2) Web Push (보조)
        try {
            webPushService.sendToUser(f.getFollowed(), buildFollowPayload(saved, f));
        } catch (Exception e) {
            log.warn("[NOTI] WebPush send failed (FOLLOW) id={}, err={}", saved.getId(), e.toString());
        }

        return saved;
    }

    private Map<String, Object> buildFollowPayload(NotificationEntity saved, FollowingEntity following) {
        Map<String, Object> actor = new HashMap<>();
        if (following.getFollower() != null) {
            if (following.getFollower().getNickname() != null) {
                actor.put("nickname", following.getFollower().getNickname());
            }
            if (following.getFollower().getProfilePhotoUrl() != null) {
                actor.put("profilePhotoUrl", following.getFollower().getProfilePhotoUrl());
            }
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "FOLLOW");
        payload.put("notificationId", saved.getId());
        payload.put("actor", actor);
        payload.put("text", "Started following you");
        return payload;
    }

    // ===================== 댓글 알림 (REQUIRES_NEW + 안전 재조회) =====================
    @Transactional(transactionManager = "transactionManager", propagation = Propagation.REQUIRES_NEW)
    public NotificationEntity createCommentAndPush(ForumCommentEntity comment) {
        Long cid = (comment != null ? comment.getId() : null);
        // [추가 로그]
        log.info("[NOTI] createCommentAndPush called. commentId={}", cid);

        if (cid == null) {
            log.warn("[NOTI] createCommentAndPush skipped: comment id is null");
            return null;
        }
        ForumCommentEntity c = forumCommentRepository.findByIdWithWriterAndForumWriter(cid)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: id=" + cid));

        final UserInfoEntity receiver = c.getForum().getWriter(); // 게시글 작성자
        final UserInfoEntity actor = c.getWriter();               // 댓글 작성자
        if (receiver.getId().equals(actor.getId())) {
            // 자기 글에 자기 댓글이면 알림 스킵
            log.info("[NOTI] COMMENT skipped (self-comment). forumWriterId={}", receiver.getId());
            return null;
        }

        NotificationEntity saved = notificationRepository.saveAndFlush(
                NotificationEntity.builder()
                        .notificationCode(NotificationCode.COMMENT)
                        .comment(c)                 // ← 다시 조회한 c 사용
                        .targetUser(receiver)
                        .checkStatus(false)
                        .build()
        );
        // [추가 로그]
        log.info("[NOTI] COMMENT saved. notiId={} targetUserId={} actorId={} forumId={}",
                saved.getId(),
                receiver.getId(),
                actor.getId(),
                (c.getForum() != null ? c.getForum().getId() : null));

        String actorNick = actor.getNickname() != null ? actor.getNickname() : "Someone";
        String preview = preview(c);
        Long forumId = c.getForum() != null ? c.getForum().getId() : null;
        // [추가 로그]
        log.info("[NOTI] COMMENT send. actorNick='{}' previewLen={} forumId={}",
                actorNick, (preview != null ? preview.length() : 0), forumId);

        // 1) FCM (최우선)
        try {
            fcmSenderService.sendComment(receiver, actorNick, preview, forumId, saved.getId());
        } catch (Exception e) {
            log.warn("[NOTI] FCM send failed (COMMENT) id={}, err={}", saved.getId(), e.toString());
        }

        // 2) Web Push (보조)
        try {
            webPushService.sendToUser(receiver, buildCommentPayload(saved, c));
        } catch (Exception e) {
            log.warn("[NOTI] WebPush send failed (COMMENT) id={}, err={}", saved.getId(), e.toString());
        }

        return saved;
    }

    private String preview(ForumCommentEntity c) {
        String src = (c != null && c.getContentsText() != null) ? c.getContentsText() : "";
        String collapsed = src.replaceAll("\\s+", " ").trim();
        if (collapsed.length() <= 140) return collapsed;
        return collapsed.substring(0, 137) + "...";
    }

    private Map<String, Object> buildCommentPayload(NotificationEntity saved, ForumCommentEntity comment) {
        Map<String, Object> actor = new HashMap<>();
        if (comment.getWriter() != null) {
            if (comment.getWriter().getNickname() != null) {
                actor.put("nickname", comment.getWriter().getNickname());
            }
            if (comment.getWriter().getProfilePhotoUrl() != null) {
                actor.put("profilePhotoUrl", comment.getWriter().getProfilePhotoUrl());
            }
        }
        Long forumId = (comment.getForum() != null) ? comment.getForum().getId() : null;

        Map<String, Object> data = new HashMap<>();
        data.put("notificationId", String.valueOf(saved.getId()));
        if (forumId != null) data.put("forumId", String.valueOf(forumId));
        data.put("actorNickname", (String) actor.getOrDefault("nickname", "Someone"));

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "COMMENT");
        payload.put("notificationId", saved.getId());
        payload.put("actor", actor);
        payload.put("title", "새 댓글");
        payload.put("body", actor.getOrDefault("nickname", "누군가") + " 님이 댓글을 남겼습니다");
        // [요구사항] 클릭 시 반드시 GET /forum/{forumId}
        payload.put("url", (forumId != null) ? ("/forum/" + forumId) : "/notification");
        payload.put("data", data);
        return payload;
    }

    // ===== 리스트/읽음 관련 =====

    @Transactional(readOnly = true)
    public NotificationListResponseDto list(Long targetUserId, int page, int size) {
        UserInfoEntity target = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        List<NotificationEntity> entities =
                notificationRepository.findUnifiedListByTarget(target, PageRequest.of(page, size));

        List<NotificationItemResponseDto> items = entities.stream()
                .map(NotificationItemResponseDto::from)
                .toList();

        long unreadCount = notificationRepository.countByTargetUserAndCheckStatusIsFalse(target);
        return NotificationListResponseDto.of(items, unreadCount);
    }

    @Transactional(transactionManager = "transactionManager")
    public int markRead(Long targetUserId, List<Long> notificationIds) {
        if (notificationIds == null || notificationIds.isEmpty()) return 0;
        UserInfoEntity target = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        return notificationRepository.markReadByIds(target, notificationIds);
    }

    @Transactional(transactionManager = "transactionManager")
    public int markAllRead(Long targetUserId) {
        UserInfoEntity target = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        return notificationRepository.markAllRead(target);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long targetUserId) {
        UserInfoEntity target = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        return notificationRepository.countByTargetUserAndCheckStatusIsFalse(target);
    }
}
