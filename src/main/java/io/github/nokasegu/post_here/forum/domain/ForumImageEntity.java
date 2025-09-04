package io.github.nokasegu.post_here.forum.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "forum_img")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "img_pk")
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "forum_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_forum_img_forum"))
    private ForumEntity forum;

    @Setter
    @Column(name = "img_url", nullable = false, length = 500)
    private String imgUrl;
}
