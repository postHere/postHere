package io.github.nokasegu.post_here.find.domain;

import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "find")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class FindEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "find_pk")
    private Long id;


    @ManyToOne
    @JoinColumn(name = "writer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_find_writer"))
    private UserInfoEntity writer;


    @Column(name = "coordinates", nullable = false)
    private Point coordinates;


    @Column(name = "content_capture_url", length = 500)
    private String contentCaptureUrl;


    @Column(name = "music_api_url", length = 500)
    private String musicApiUrl;


    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;


    @Column(name = "readonly")
    private Boolean readOnly;


    @Column(name = "content_overwrite_url", length = 500)
    private String contentOverwriteUrl;


    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
