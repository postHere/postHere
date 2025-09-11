package io.github.nokasegu.post_here.userInfo.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * UserInfoEntity
 * <p>
 * 역할(정확 명칭)
 * - 서버: 애플리케이션의 기본 사용자 프로필/계정 엔티티
 * · id: 사용자 PK
 * · email: 로그인/연락용 이메일
 * · loginPw: 로그인 비밀번호(저장 시 해시 전제)
 * · nickname: 서비스 내 표시명
 * · profilePhotoUrl: 프로필 이미지 경로/URL
 * · createdAt/updatedAt: 생성/수정 시각(JPA Auditing)
 * <p>
 * 사용처(정확 명칭)
 * - 팔로우 관계: FollowingEntity.follower / FollowingEntity.followed 에서 참조
 * - 알림 수신자: NotificationEntity.targetUser 에서 참조
 * - Web Push 구독 주체: PushSubscriptionEntity.user (구독 목록 조회/전송 대상 식별)
 * <p>
 * 보안/설계 메모
 * - loginPw: 반드시 해시(예: BCrypt)로 저장하고 평문/가역암호 금지.
 * - email: 로그인/중복 체크에 사용하면 DB 유니크 제약 권장.
 * - nickname: 검색/정렬에 자주 쓰이면 인덱스 고려.
 * - profilePhotoUrl: 외부 URL 저장 시 길이/검증 정책 필요(현재 length=500).
 * - Auditing 사용: @EnableJpaAuditing 활성화 전제, createdAt은 updatable=false.
 */
@Entity
@Table(name = "user_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserInfoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_info_pk")
    private Long id;

    /**
     * 로그인/연락용 이메일(유니크 제약 권장)
     */
    @Column(name = "email", nullable = false)
    private String email;

    /**
     * 로그인 비밀번호(해시 전제: 평문 저장 금지)
     */
    @Column(name = "login_pw", nullable = false)
    private String loginPw;

    /**
     * 서비스 내 표시명
     */
    @Column(name = "nickname", nullable = false, length = 60)
    private String nickname;

    /**
     * 프로필 이미지 URL/경로
     */
    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    /**
     * 생성 시각(Auditing) — 수정 불가
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 최종 수정 시각(Auditing)
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}