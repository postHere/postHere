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

    /**
     * [변경] 목록 API
     * - 정렬은 Repository JPQL의 createdAt DESC, id DESC 를 그대로 사용한다.
     * - "FOLLOW 최신 1건만" 규칙 + "끊긴 FOLLOW(following_id NULL) 제외"를 **서비스 후처리**로 보장한다.
     * - 페이지네이션은 그대로 유지하되, **오버페치**(chunk 단위)로 필터링 손실을 보완하고 **hasNext**를 안정적으로 계산한다.
     * <p>
     * 동작 요약:
     * 1) 클라이언트의 (page,size)를 받아, 0페이지부터 chunk 단위로 순차 조회
     * 2) 각 chunk를 시간 내림차순으로 순회하며 필터링 규칙 적용:
     * - FOLLOW: following == null 이면 제외 (언팔/삭제 등으로 유령화된 알림 숨김)
     * - FOLLOW: followerId 기준 "처음 만난" 1건만 채택 (최신 1건)
     * - COMMENT 등은 그대로 채택
     * 3) 필터링 결과 수가 (page+1)*size + 1 에 도달하거나, 더 이상 chunk가 없을 때까지 반복
     * 4) 최종 필터링 스트림에서 [page*size, page*size+size) 범위를 슬라이스하여 응답
     * 5) 필터링된 전체 수가 슬라이스 끝을 초과하면 hasNext=true
     */
    @Transactional(readOnly = true)
    public NotificationListResponseDto list(Long targetUserId, int page, int size) {
        UserInfoEntity target = userInfoRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));

        // [신규] 오버페치 chunk 크기: 필터링 손실을 보완하기 위해 요청 size의 3배(최소 60) 정도로 크게
        final int chunkSize = Math.max(60, size * 3);

        // [신규] FOLLOW 최신 1건 판별을 위한 전역 Set (팔로워 id)
        final Set<Long> seenFollowerIds = new HashSet<>();

        // [신규] 필터링 누적 버퍼 (정렬 순서 유지)
        final List<NotificationEntity> filtered = new ArrayList<>();

        // [신규] 몇 개까지 모아야 하는가: 요청한 페이지의 끝 + 다음 항목(존재 여부 판단용)
        final int wantUntil = (page + 1) * size + 1;

        boolean moreRepoData = true;
        int repoPage = 0;

        while (filtered.size() < wantUntil && moreRepoData) {
            // chunk 단위로 0페이지부터 순차 조회 (정렬은 JPQL에서 DESC 고정)
            List<NotificationEntity> chunk = notificationRepository.findUnifiedListByTarget(
                    target, PageRequest.of(repoPage, chunkSize));

            // 더 이상 데이터가 없으면 종료
            if (chunk == null || chunk.isEmpty()) {
                moreRepoData = false;
                break;
            }

            // === [신규] 후처리 필터링 ===
            for (NotificationEntity n : chunk) {
                final NotificationCode code = n.getNotificationCode();

                if (code == NotificationCode.FOLLOW) {
                    // 끊긴 FOLLOW(연결 NULL) 제외
                    FollowingEntity f = n.getFollowing();
                    if (f == null || f.getFollower() == null) {
                        continue;
                    }
                    Long followerId = f.getFollower().getId();
                    if (followerId == null) continue;

                    // 팔로워별 최신 1건만 채택 (가장 먼저 만나는 것이 최신)
                    if (seenFollowerIds.contains(followerId)) continue;
                    seenFollowerIds.add(followerId);
                    filtered.add(n);
                } else {
                    // 그 외 타입은 그대로 채택
                    filtered.add(n);
                }

                if (filtered.size() >= wantUntil) break;
            }

            // 다음 chunk로
            repoPage++;

            // chunk 사이즈 미만을 반환하면, 더 이상 다음 페이지 없음으로 간주
            if (chunk.size() < chunkSize) {
                moreRepoData = false;
            }
        }

        // === [신규] 페이지 슬라이싱 ===
        final int start = Math.min(page * size, filtered.size());
        final int end = Math.min(start + size, filtered.size());
        List<NotificationEntity> pageSlice = filtered.subList(start, end);

        // === [신규] hasNext 계산 ===
        boolean hasNext = filtered.size() > end;
        // (보수적으로) 필터링 결과가 아직 end를 넘지 못했지만 저장소에 더 있을 수 있으면 true로 보정할 수도 있음.
        // 여기서는 wantUntil(=end+1)까지 충분히 모을 때까지 loop를 돌렸으므로 위 계산이 안정적이다.

        // DTO 변환
        List<NotificationItemResponseDto> items = pageSlice.stream()
                .map(NotificationItemResponseDto::from)
                .toList();

        long unreadCount = notificationRepository.countByTargetUserAndCheckStatusIsFalse(target);

        // [변경] hasNext 포함 응답
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
