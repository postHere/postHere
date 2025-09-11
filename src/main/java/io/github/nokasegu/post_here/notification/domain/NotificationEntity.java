package io.github.nokasegu.post_here.notification.domain;

import io.github.nokasegu.post_here.find.domain.FindEntity;
import io.github.nokasegu.post_here.follow.domain.FollowingEntity;
import io.github.nokasegu.post_here.forum.domain.ForumCommentEntity;
import io.github.nokasegu.post_here.park.domain.ParkEntity;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * NotificationEntity
 * <p>
 * [역할(정확 명칭)]
 * - 서버: 개별 알림 레코드의 영속 모델
 * · 어떤 이벤트가 발생했는지: {@link NotificationCode}
 * · 어떤 리소스에 대한 알림인지: following/comment/find/park 중 0~1개 연관
 * · 누구에게 전달되는지: targetUser
 * · 읽음 상태 및 생성 시각: checkStatus, createdAt
 * <p>
 * [사용처(정확 명칭)]
 * - 서버: NotificationService.createFollowAndPush(...) 등에서 INSERT
 * - 서버: NotificationRepository.findListByTarget(...)로 목록 조회(알림센터 UI 렌더용)
 * - 서버: NotificationRepository.*Read*(...)로 읽음 상태 업데이트
 * - 클라이언트: /notification 리스트/읽음/배지 갱신에 간접 사용
 * <p>
 * [설계 메모]
 * - 연관 선택 규칙: notificationCode에 따라 following/comment/find/park 중 하나만 사용하도록
 * 서비스 계층에서 관례를 유지(엔티티 차원 강제는 아님).
 * - 읽음 상태(checkStatus): 저장 시 기본적으로 false(미읽음)로 설정 후, 읽음 API에서 true로 전환.
 * - 생성 시각(createdAt): JPA Auditing(@EnableJpaAuditing 필요)에 의해 자동 기록.
 * - 정렬/쿼리: 목록 조회는 보통 createdAt DESC, targetUser 조건으로 필터링.
 * - 인덱스 권장(DB 스키마 차원): (target_user_id, check_status), (target_user_id, created_at)
 * (엔티티에는 명시하지 않았으며, 실제 운영 스키마에서 추가 구성 권장)
 */
@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_pk")
    private Long id;

    /**
     * 알림 유형(FOLLOW, COMMENT, FIND, PARK 등)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_code", nullable = false, length = 50)
    private NotificationCode notificationCode;

    /**
     * 팔로우 알림일 때 참조(누가 누구를 팔로우했는지) — 선택적 연관
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id",
            foreignKey = @ForeignKey(name = "fk_notification_following"))
    private FollowingEntity following;

    /**
     * 댓글 알림일 때 참조 — 선택적 연관
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id",
            foreignKey = @ForeignKey(name = "fk_notification_comment"))
    private ForumCommentEntity comment;

    /**
     * Fin'd(찾기) 알림일 때 참조 — 선택적 연관
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "find_id",
            foreignKey = @ForeignKey(name = "fk_notification_find"))
    private FindEntity find;

    /**
     * Park 알림일 때 참조 — 선택적 연관
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "park_id",
            foreignKey = @ForeignKey(name = "fk_notification_park"))
    private ParkEntity park;

    /**
     * 알림 수신자(반드시 존재)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_target_user"))
    private UserInfoEntity targetUser;

    /**
     * 읽음 상태 플래그: false=미읽음, true=읽음 (저장 시 서비스에서 false로 세팅)
     */
    @Column(name = "check_status", nullable = false)
    private Boolean checkStatus; // false = unread, true = read

    /**
     * 생성 시각(Auditing) — 수정 불가
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
