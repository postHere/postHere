package io.github.nokasegu.post_here.follow.domain;

import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "following")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class FollowingEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "following_pk")
    private Long id;


    @ManyToOne
    @JoinColumn(name = "follower_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_following_follower"))
    private UserInfoEntity follower;


    @ManyToOne
    @JoinColumn(name = "followed_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_following_followed"))
    private UserInfoEntity followed;


    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}