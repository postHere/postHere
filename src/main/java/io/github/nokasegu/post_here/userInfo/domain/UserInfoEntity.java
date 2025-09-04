package io.github.nokasegu.post_here.userInfo.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

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


    @Column(name = "email", nullable = false)
    private String email;


    @Column(name = "login_pw", nullable = false)
    private String loginPw;


    @Column(name = "nickname", nullable = false, length = 60)
    private String nickname;


    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;


    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}