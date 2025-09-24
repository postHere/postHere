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
 * - endpoint는 각 브라우저가 발급하는 고유 식별자이므로 유니크 제약(uq_push_sub_endpoint)으로 중복 등록을 방지한다.
 * - user_id 인덱스(idx_push_sub_user)로 사용자 단위 조회 성능을 확보한다.
 * - endpoint 길이는 브라우저/벤더에 따라 길어질 수 있어 여유 있게 1000자로 설정.
 * - p256dh/auth는 base64url 형식의 키 문자열이므로 255자로 설정(일반적으로 87~88자 수준).
 * <p>
 * [감사 필드]
 * - createdAt/updatedAt은 JPA Auditing(@EnableJpaAuditing 필요)에 의해 자동 갱신된다.
 * <p>
 * [연관관계]
 * - N:1(UserInfoEntity) LAZY 로딩. 사용자 삭제 정책은 도메인 요구에 따라 별도 처리(연쇄 삭제/보존 등).
 * <p>
 * [확장 포인트]
 * - 기기 타입(OS/브라우저), 구독 상태(활성/비활성), 마지막 성공/실패 시각, 실패 누적 카운트 등의 운영 필드 추가 가능.
 */
@Entity
@Table(
        name = "push_subscription",
        indexes = {@Index(name = "idx_push_sub_user", columnList = "user_id")},
        uniqueConstraints = {@UniqueConstraint(name = "uq_push_sub_endpoint", columnNames = {"endpoint"})}
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
