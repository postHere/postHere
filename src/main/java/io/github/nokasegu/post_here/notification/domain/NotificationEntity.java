package io.github.nokasegu.post_here.notification.domain;

import io.github.nokasegu.post_here.find.domain.FindEntity;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class NotificationEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "find_id",
            foreignKey = @ForeignKey(name = "fk_notification_find"))
    private FindEntity find;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "park_id",
            foreignKey = @ForeignKey(name = "fk_notification_park"))
    private ParkEntity park;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_target_user"))
    private UserInfoEntity targetUser;


    @Column(name = "check_status", nullable = false)
    private Boolean checkStatus; // false = unread, true = read


    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}