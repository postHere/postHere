package io.github.nokasegu.post_here.forum.domain;

import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "forum_like")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ForumLikeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "forum_like_pk")
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "forum_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_like_forum"))
    private ForumEntity forum;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "forum_liker_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_like_user"))
    private UserInfoEntity liker;


    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
