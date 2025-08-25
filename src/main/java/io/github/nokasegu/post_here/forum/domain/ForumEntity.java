package io.github.nokasegu.post_here.forum.domain;

import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Entity
@Table(name = "forum")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ForumEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "forum_pk")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "writer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_forum_writer"))
    private UserInfoEntity writer;


    @Column(name = "location")
    private String location;


    @Column(name = "contents_text", columnDefinition = "text")
    private String contentsText;


    @Column(name = "music_api_url", length = 500)
    private String musicApiUrl;

    @CreatedDate                   // 생성 시 자동 저장
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate              // 수정 시 자동 갱신
    @Column(nullable = false)
    private LocalDateTime updatedDate;
}
