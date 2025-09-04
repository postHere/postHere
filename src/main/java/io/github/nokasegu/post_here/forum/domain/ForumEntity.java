package io.github.nokasegu.post_here.forum.domain;

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
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "forum")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ForumEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "forum_pk")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "writer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_forum_writer"))
    private UserInfoEntity writer;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location", nullable = false)
    private ForumAreaEntity location;


    @Column(name = "contents_text", columnDefinition = "text")
    private String contentsText;


    @Column(name = "music_api_url", length = 500)
    private String musicApiUrl;

    @CreatedDate                   // 생성 시 자동 저장
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate              // 수정 시 자동 갱신
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ForumImageEntity와 1:N 관계 설정
    @OneToMany(mappedBy = "forum", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ForumImageEntity> images = new ArrayList<>();

    // 편의 메서드: 이미지 추가
    public void addImage(ForumImageEntity image) {
        images.add(image);
        image.setForum(this); // ForumImageEntity에 ForumEntity를 설정 (양방향 관계)
    }
}
