package io.github.nokasegu.post_here.forum.dto;

import io.github.nokasegu.post_here.forum.domain.ForumEntity;
import io.github.nokasegu.post_here.forum.domain.ForumImageEntity;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Setter
public class ForumPostListResponseDto {

    private final Long id;
    private final String location;
    private final String contentsText;
    private final String writerNickname;
    private final Long writerId;
    private final List<String> imageUrls;
    private final String writerProfilePhotoUrl;
    private final int totalComments;
    private final boolean author;
    private final LocalDateTime createdAt;

    // 좋아요 관련 필드
    private final int totalLikes;
    private final boolean isLiked;
    private final List<String> recentLikerPhotos;

    public ForumPostListResponseDto(
            ForumEntity forumEntity,
            int totalComments,
            LocalDateTime createdAt,
            int totalLikes,
            boolean isLiked,
            List<String> recentLikerPhotos,
            boolean author
    ) {
        this.id = forumEntity.getId();
        // ForumAreaEntity 객체에서 address 필드를 가져와 String으로 설정
        this.location = forumEntity.getLocation().getAddress();
        this.contentsText = forumEntity.getContentsText();
        this.writerNickname = forumEntity.getWriter().getNickname();
        this.writerId = forumEntity.getWriter().getId();
        this.imageUrls = forumEntity.getImages().stream()
                .map(ForumImageEntity::getImgUrl)
                .collect(Collectors.toList());
        this.writerProfilePhotoUrl = forumEntity.getWriter().getProfilePhotoUrl();
        this.totalComments = totalComments;
        this.createdAt = forumEntity.getCreatedAt();
        this.totalLikes = totalLikes;
        this.isLiked = isLiked;
        this.recentLikerPhotos = recentLikerPhotos;
        this.author = author;

        // writer null 안전 처리
        if (forumEntity.getWriter() != null) {
            this.writerNickname = forumEntity.getWriter().getNickname();
            this.writerId = forumEntity.getWriter().getId();
            this.writerProfilePhotoUrl = forumEntity.getWriter().getProfilePhotoUrl();
        } else {
            this.writerNickname = "알 수 없는 사용자";
            this.writerId = -1L;
            this.writerProfilePhotoUrl = "/images/default-profile.png";
        }

        // location null 안전 처리 (현행 로직 유지: 주소 전체 사용)
        if (forumEntity.getLocation() != null) {
            this.location = forumEntity.getLocation().getAddress();
        } else {
            this.location = "위치 정보 없음";
        }

        // 🔧 핵심 수정: 이미지 URL에서 null/빈 문자열 제거 → 텍스트만 있는 글이면 빈 리스트가 되어 캐러셀 미표시
        List<String> urls = (forumEntity.getImages() == null) ? Collections.emptyList()
                : forumEntity.getImages().stream()
                .map(ForumImageEntity::getImgUrl)
                .filter(Objects::nonNull)                 // 진짜 null 제거
                .map(String::trim)
                .filter(s -> !s.isEmpty())                // 빈 문자열 제거
                .filter(s -> !"null".equalsIgnoreCase(s)) // 문자열 "null" 제거 ★
                .collect(Collectors.toList());

        this.imageUrls = urls.isEmpty() ? Collections.emptyList() : urls;
    }
}
