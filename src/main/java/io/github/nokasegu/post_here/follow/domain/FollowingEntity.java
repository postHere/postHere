package io.github.nokasegu.post_here.follow.domain;

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
 * FollowingEntity
 * <p>
 * 역할(정확 명칭)
 * - 서버: “누가(follower) 누구를(followed) 팔로우했는지”를 나타내는 팔로우 관계 레코드의 영속 모델
 * · follower: 팔로우를 건 사용자
 * · followed: 팔로우를 당한(=대상) 사용자
 * · createdAt: 관계가 생성된 시각(JPA Auditing)
 * <p>
 * 사용처(정확 명칭)
 * - 서버: FollowingService.follow(...)에서 INSERT(멱등 검사 후 생성)
 * - 서버: FollowingService.unfollow(...)에서 DELETE
 * - 서버: NotificationService.createFollowAndPush(...)에서 “팔로우 알림” 생성 시 이 엔티티를 참조
 * <p>
 * 설계 메모
 * - 무결성: (follower_id, followed_id) 유니크 제약을 DB에 두는 것을 권장(중복 삽입 방지)
 * - 자기 자신 팔로우 금지: 서비스 계층에서 파라미터 검증으로 차단
 * - 조회 성능: me의 팔로워/팔로잉 목록 조회가 빈번하므로 (follower_id), (followed_id) 인덱스 권장
 * - 생성 시각(createdAt): @EnableJpaAuditing 활성화 필요, updatable=false로 변경 불가
 */
@Entity
@Table(name = "following")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class FollowingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "following_pk")
    private Long id;

    /**
     * 팔로우를 “건” 사용자
     */
    @ManyToOne
    @JoinColumn(name = "follower_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_following_follower"))
    private UserInfoEntity follower;

    /**
     * 팔로우의 “대상” 사용자
     */
    @ManyToOne
    @JoinColumn(name = "followed_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_following_followed"))
    private UserInfoEntity followed;

    /**
     * 관계 생성 시각(Auditing) — 수정 불가
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
