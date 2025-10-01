package io.github.nokasegu.post_here.find.dto;

import io.github.nokasegu.post_here.find.domain.FindEntity;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FindDetailViewDto {
    private Long id;
    private String writerNickname;
    private String writerProfilePhotoUrl;
    private String contentCaptureUrl; // Fin'd의 메인 이미지
    private String locationName;      // 위치 정보
    private LocalDateTime createdAt;
    private boolean isAuthor;

    public FindDetailViewDto(FindEntity entity, Long currentUserId) {
        this.id = entity.getId();
        this.writerNickname = entity.getWriter().getNickname();
        this.writerProfilePhotoUrl = entity.getWriter().getProfilePhotoUrl();
        this.contentCaptureUrl = entity.getContentCaptureUrl();
        this.locationName = "서울시 강남구"; // TODO: 좌표를 주소로 변환하는 로직 필요
        this.createdAt = entity.getCreatedAt();

        if (currentUserId != null) {
            this.isAuthor = entity.getWriter().getId().equals(currentUserId);
        } else {
            this.isAuthor = false;
        }
    }
}

