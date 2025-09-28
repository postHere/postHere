package io.github.nokasegu.post_here.notification.domain;

import io.github.nokasegu.post_here.follow.domain.FollowingEntity;
import io.github.nokasegu.post_here.forum.domain.ForumCommentEntity;
import io.github.nokasegu.post_here.park.domain.ParkEntity;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_pk")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_code", nullable = false, length = 50)
    private NotificationCode notificationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_id",
            foreignKey = @ForeignKey(name = "fk_notification_following"))
    private FollowingEntity following;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id",
            foreignKey = @ForeignKey(name = "fk_notification_comment"))
    private ForumCommentEntity comment;

    //현재 위치에서 조회된 fin'd가 여러 개일 경우 연관 관계를 맺으려면 별도의 테이블이 필요함
    //fin'd 발견 알림을 누르면 항상 Map으로 이동되도록 동작하기 때문에 굳이 fin'd를 저장할 필요가 없음 메세지 내용 자체만 저장
    @Column(name = "message_for_find")
    private String messageForFind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "park_id",
            foreignKey = @ForeignKey(name = "fk_notification_park"))
    private ParkEntity park;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_target_user"))
    private UserInfoEntity targetUser;

    @Column(name = "check_status", nullable = false)
    private Boolean checkStatus;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "timestamp default current_timestamp")
    private LocalDateTime createdAt;

    // ====== Builder 보정 ======
    @PrePersist
    public void prePersist() {
        if (checkStatus == null) {
            checkStatus = Boolean.FALSE;
        }
    }
}
