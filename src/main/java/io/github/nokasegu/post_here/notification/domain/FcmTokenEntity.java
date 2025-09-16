package io.github.nokasegu.post_here.notification.domain;

import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * FcmTokenEntity
 * <p>
 * 역할
 * - 사용자 1:N 기기 토큰을 저장 (Android/iOS 등)
 * <p>
 * 설계
 * - token 유니크 제약으로 중복 저장 방지
 * - user 인덱스
 */
@Entity
@Table(
        name = "fcm_token",
        uniqueConstraints = {@UniqueConstraint(name = "uq_fcm_token_token", columnNames = {"token"})},
        indexes = {@Index(name = "idx_fcm_token_user", columnList = "user_id")}
)
@Getter
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

    @Column(name = "token", nullable = false, length = 4096)
    private String token;

    @Column(name = "platform", length = 30) // "android", "ios" 등
    private String platform;

    @Column(name = "app_name", length = 100) // "post-here"
    private String appName;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
