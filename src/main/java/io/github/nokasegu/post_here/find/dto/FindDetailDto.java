package io.github.nokasegu.post_here.find.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FindDetailDto {
    private Long findId;
    private String writerNickname;
    private String writerProfilePhotoUrl;
    private String contentCaptureUrl;
    private String locationName;
    private boolean isAuthor;
    private String createdAt; // "yyyy.MM.dd HH:mm" 형식
    private String remainingTime; // "HH:mm 남음" 형식
}

