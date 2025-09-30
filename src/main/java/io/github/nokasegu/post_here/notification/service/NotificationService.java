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

import java.util.*;

/**
 * NotificationService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserInfoRepository userInfoRepository;
    private final WebPushService webPushService;
    private final FcmSenderService fcmSenderService;

    // 안전 재조회용
    private final ForumCommentRepository forumCommentRepository;
    private final FollowingRepository followingRepository;

    // ============================================================
    // FOLLOW 생성 + 푸시
    // ============================================================
    @Transactional(transactionManager = "transactionManager", propagation = Propagation.REQUIRES_NEW)
    public NotificationEntity createFollowAndPush(FollowingEntity following) {
        Long fid = (following != null ? following.getId() : null);
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
                        .following(f)
                        .targetUser(f.getFollowed())
                        .checkStatus(false)
                        .build()
        );
        log.info("[NOTI] FOLLOW saved. notiId={} targetUserId={} actorId={}",
                saved.getId(),
                (f.getFollowed() != null ? f.getFollowed().getId() : null),
                (f.getFollower() != null ? f.getFollower().getId() : null));

        // 1) FCM
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

        // 2) Web Push
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

    // ============================================================
    // COMMENT 생성 + 푸시
    // ============================================================
    @Transactional(transactionManager = "transactionManager", propagation = Propagation.REQUIRES_NEW)
    public NotificationEntity createCommentAndPush(ForumCommentEntity comment) {
        Long cid = (comment != null ? comment.getId() : null);
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
            log.info("[NOTI] COMMENT skipped (self-comment). forumWriterId={}", receiver.getId());
            return null;
        }

        NotificationEntity saved = notificationRepository.saveAndFlush(
                NotificationEntity.builder()
                        .notificationCode(NotificationCode.COMMENT)
                        .comment(c)
                        .targetUser(receiver)
                        .checkStatus(false)
                        .build()
        );
        log.info("[NOTI] COMMENT saved. notiId={} targetUserId={} actorId={} forumId={}",
                saved.getId(),
                receiver.getId(),
                actor.getId(),
                (c.getForum() != null ? c.getForum().getId() : null));

        String actorNick = actor.getNickname() != null ? actor.getNickname() : "Someone";
        String preview = preview(c);
        Long forumId = c.getForum() != null ? c.getForum().getId() : null;

        // 1) FCM
        try {
            fcmSenderService.sendComment(receiver, actorNick, preview, forumId, saved.getId());
        } catch (Exception e) {
            log.warn("[NOTI] FCM send failed (COMMENT) id={}, err={}", saved.getId(), e.toString());
        }

        // 2) Web Push (URL도 상세로 직접 연결: 댓글 모달 자동 오픈)
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
        Long commentId = comment.getId();

        Map<String, Object> data = new HashMap<>();
        data.put("notificationId", String.valueOf(saved.getId()));
        if (forumId != null) data.put("forumId", String.valueOf(forumId));
        if (commentId != null) data.put("commentId", String.valueOf(commentId));
        data.put("actorNickname", (String) actor.getOrDefault("nickname", "Someone"));

        // OS 알림 클릭 시에도 댓글 모달이 열리고 대상 댓글로 이동하도록 URL 구성
        String url = (forumId != null)
                ? ("/forum/" + forumId +
                (commentId != null ? ("?open=comments&commentId=" + commentId + "#comment-" + commentId)
                        : "?open=comments"))
                : "/notification";

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "COMMENT");
        payload.put("notificationId", saved.getId());
        payload.put("actor", actor);
        payload.put("title", "새 댓글");
        payload.put("body", actor.getOrDefault("nickname", "누군가") + " 님이 댓글을 남겼습니다");
        payload.put("url", url);     // ★ 상세로 직결
        payload.put("data", data);
        return payload;
    }

    // ============================================================
    // 목록 / 읽음 / 카운트
    // ============================================================
    @Transactional(readOnly = true)
    public NotificationListResponseDto list(Long targetUserId, int page, int size) {
        UserInfoEntity target = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        final int chunkSize = Math.max(60, size * 3);
        final Set<Long> seenFollowerIds = new HashSet<>();
        final List<NotificationEntity> filtered = new ArrayList<>();
        final int wantUntil = (page + 1) * size + 1;

        boolean moreRepoData = true;
        int repoPage = 0;

        while (filtered.size() < wantUntil && moreRepoData) {
            List<NotificationEntity> chunk = notificationRepository.findUnifiedListByTarget(
                    target, PageRequest.of(repoPage, chunkSize));

            if (chunk == null || chunk.isEmpty()) {
                moreRepoData = false;
                break;
            }

            for (NotificationEntity n : chunk) {
                final NotificationCode code = n.getNotificationCode();

                if (code == NotificationCode.FOLLOW) {
                    FollowingEntity f = n.getFollowing();
                    if (f == null || f.getFollower() == null) {
                        continue; // 유령 FOLLOW 제외
                    }
                    Long followerId = f.getFollower().getId();
                    if (followerId == null) continue;

                    if (seenFollowerIds.contains(followerId)) continue; // 최신 1건만
                    seenFollowerIds.add(followerId);
                    filtered.add(n);
                } else {
                    filtered.add(n);
                }

                if (filtered.size() >= wantUntil) break;
            }

            repoPage++;
            if (chunk.size() < chunkSize) {
                moreRepoData = false;
            }
        }

        final int start = Math.min(page * size, filtered.size());
        final int end = Math.min(start + size, filtered.size());
        List<NotificationEntity> pageSlice = filtered.subList(start, end);
        boolean hasNext = filtered.size() > end;

        // DTO 변환 + 링크 주입
        List<NotificationItemResponseDto> items = new ArrayList<>(pageSlice.size());
        for (NotificationEntity n : pageSlice) {
            NotificationItemResponseDto dto = NotificationItemResponseDto.from(n);

            // === 여기서 링크 및 코멘트 미리보기 보강 ===
            if (n.getNotificationCode() == NotificationCode.COMMENT && n.getComment() != null) {
                Long forumId = (n.getComment().getForum() != null) ? n.getComment().getForum().getId() : null;
                Long commentId = n.getComment().getId();

                if (forumId != null) {
                    String link = "/forum/" + forumId;
                    dto.setLink(link);
                }

                // 댓글 미리보기(선택)
                String prev = n.getComment().getContentsText();
                if (prev != null) {
                    prev = prev.replaceAll("\\s+", " ").trim();
                    if (prev.length() > 140) prev = prev.substring(0, 137) + "...";
                    dto.setCommentPreview(prev);
                }
            } else if (n.getNotificationCode() == NotificationCode.FIND_FOUND) {
                dto.setLink("/find/on-map");
            }

            items.add(dto);
        }

        long unreadCount = notificationRepository.countByTargetUserAndCheckStatusIsFalse(target);
        return NotificationListResponseDto.of(items, unreadCount, hasNext);
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

    public void createFind(UserInfoEntity target, String message) {
        NotificationEntity notification = NotificationEntity.builder()
                .notificationCode(NotificationCode.FIND_FOUND)
                .targetUser(target)
                .messageForFind(message)
                .checkStatus(false)
                .build();

        notificationRepository.save(notification);
    }
}
