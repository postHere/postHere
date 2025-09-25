// src/main/java/io/github/nokasegu/post_here/notification/domain/PushSubscriptionEntity.java
package io.github.nokasegu.post_here.notification.domain;

import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * PushSubscriptionEntity
 * <p>
 * [역할]
 * - 브라우저(Web Push API)가 발급한 구독 정보(endpoint, p256dh, auth)를 사용자(UserInfoEntity)에 매핑해 영속화한다.
 * - WebPushService.sendToUser(...)가 특정 사용자에게 푸시 전송 시 이 테이블을 조회한다.
 * <p>
 * [무결성/제약]
 * - (user_id, endpoint) 복합 유니크 제약으로 같은 브라우저 endpoint라도 유저마다 별도 구독 가능.
 * - user_id 인덱스로 사용자 단위 조회 성능 확보.
 */
@Entity
@Table(
        name = "push_subscription",
        indexes = {@Index(name = "idx_push_sub_user", columnList = "user_id")},
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_push_sub_user_endpoint", columnNames = {"user_id", "endpoint"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PushSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_subscription_pk")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_push_sub_user"))
    private UserInfoEntity user;

    @Column(name = "endpoint", nullable = false, length = 1000)
    private String endpoint;

    @Column(name = "p256dh", nullable = false, length = 255)
    private String p256dh;

    @Column(name = "auth", nullable = false, length = 255)
    private String auth;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
