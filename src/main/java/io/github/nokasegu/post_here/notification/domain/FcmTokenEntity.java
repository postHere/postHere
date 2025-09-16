package io.github.nokasegu.post_here.notification.domain;

import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * FcmTokenEntity
 * - 사용자 디바이스의 FCM registration token을 저장하는 엔티티
 * - token에 유니크 제약을 두어 멱등 upsert를 지원
 */
@Entity
@Table(
        name = "fcm_token",
        indexes = {@Index(name = "idx_fcm_token_user", columnList = "user_id")},
        uniqueConstraints = {@UniqueConstraint(name = "uq_fcm_token_token", columnNames = {"token"})}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class FcmTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fcm_token_pk")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_fcm_token_user"))
    private UserInfoEntity user;

    @Column(name = "token", nullable = false, length = 2048)
    private String token;

    @Column(name = "platform", length = 32)
    private String platform;

    @Column(name = "app", length = 64)
    private String app;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
