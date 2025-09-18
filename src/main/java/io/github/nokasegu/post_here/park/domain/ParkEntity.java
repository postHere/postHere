package io.github.nokasegu.post_here.park.domain;

import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "park")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ParkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "park_pk")
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_park_owner"))
    private UserInfoEntity owner;


    @Column(name = "content_capture_url", length = 500)
    private String contentCaptureUrl;


    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void updateUrl(String newUrl) {
        this.contentCaptureUrl = newUrl;
    }
}
