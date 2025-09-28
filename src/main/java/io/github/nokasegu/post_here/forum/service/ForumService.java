package io.github.nokasegu.post_here.forum.service;

import io.github.nokasegu.post_here.forum.domain.ForumAreaEntity;
import io.github.nokasegu.post_here.forum.domain.ForumCommentEntity;
import io.github.nokasegu.post_here.forum.domain.ForumEntity;
import io.github.nokasegu.post_here.forum.dto.*;
import io.github.nokasegu.post_here.forum.repository.ForumAreaRepository;
import io.github.nokasegu.post_here.forum.repository.ForumCommentRepository;
import io.github.nokasegu.post_here.forum.repository.ForumLikeRepository;
import io.github.nokasegu.post_here.forum.repository.ForumRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ForumService {

    private final ForumRepository forumRepository;
    private final UserInfoRepository userInfoRepository;
    private final ForumAreaRepository forumAreaRepository;
    private final ForumImageService forumImageService;
    private final ForumCommentRepository forumCommentRepository;
    private final ForumLikeRepository forumLikeRepository;

    public ForumCreateResponseDto createForum(ForumCreateRequestDto requestDto) throws IOException {
        UserInfoEntity writer = userInfoRepository.findByEmail(requestDto.getUserEmail())
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));
        ForumAreaEntity area = forumAreaRepository.findById(requestDto.getLocation())
                .orElseThrow(() -> new EntityNotFoundException("유효하지 않은 지역입니다."));
        ForumEntity forum = ForumEntity.builder()
                .writer(writer)
                .location(area)
                .contentsText(requestDto.getContent())
                .createdAt(LocalDateTime.now())
                .build();
        ForumEntity savedForum = forumRepository.save(forum);

        // 2. 이미지 URL 목록을 사용하여 이미지를 DB에 저장하고 게시글과 연결
        if (requestDto.getImageUrls() != null && !requestDto.getImageUrls().isEmpty()) {
            for (String imageUrl : requestDto.getImageUrls()) {
                forumImageService.saveImage(imageUrl, savedForum);
            }
        }
        return new ForumCreateResponseDto(savedForum.getId());
    }

    /**
     * 게시글을 수정합니다.
     *
     * @param forumId    수정할 게시글 ID
     * @param requestDto 수정 데이터
     * @param userId     현재 사용자 ID (권한 확인용)
     */
    public void updateForum(Long forumId, ForumUpdateRequestDto requestDto, Long userId) {
        ForumEntity forum = getForumEntityAndCheckPermission(forumId, userId);
        forum.setContentsText(requestDto.getContent());
        forum.setUpdatedAt(LocalDateTime.now());
        if (requestDto.getDeletedImageIds() != null && !requestDto.getDeletedImageIds().isEmpty()) {
            forumImageService.deleteImagesByIds(requestDto.getDeletedImageIds(), userId);
        }
        forumRepository.save(forum);
    }

    /**
     * 게시글을 삭제합니다.
     *
     * @param forumId 삭제할 게시글 ID
     * @param userId  현재 사용자 ID (권한 확인용)
     */
    public void deleteForum(Long forumId, Long userId) {
        ForumEntity forum = getForumEntityAndCheckPermission(forumId, userId);
        forumImageService.deleteImages(forum);
        forumRepository.delete(forum);
    }

    /**
     * 게시글의 상세 정보를 조회하고, 작성자 여부를 확인합니다.
     * 이 메소드는 게시글 수정 페이지 로딩 시 사용됩니다.
     *
     * @param forumId       조회할 게시글 ID
     * @param currentUserId 현재 사용자 ID
     * @return 게시글 상세 정보 DTO
     */
    public ForumDetailResponseDto getForumDetail(Long forumId, Long currentUserId) {
        ForumEntity forum = getForumEntityAndCheckPermission(forumId, currentUserId);
        return new ForumDetailResponseDto(forum, true);
    }

    /**
     * 주어진 ID로 게시글을 찾고, 현재 사용자가 작성자인지 권한을 확인하는 공통 메서드
     *
     * @param forumId 확인할 게시글 ID
     * @param userId  현재 사용자 ID
     * @return 권한이 확인된 ForumEntity
     */
    private ForumEntity getForumEntityAndCheckPermission(Long forumId, Long userId) {
        ForumEntity forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 게시글입니다."));
        if (userId != null && forum.getWriter() != null && !forum.getWriter().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 게시글에 대한 수정/삭제 권한이 없습니다.");
        }
        return forum;
    }

    /**
     * 주소 문자열을 사용하여 지역의 PK (ID)를 조회합니다.
     *
     * @param address 조회할 지역의 주소 문자열
     * @return 해당 지역의 PK (ID)
     */
    public ForumAreaEntity getAreaByAddress(String address) {
        return forumAreaRepository.findByAddress(address)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역 정보입니다."));
    }

    /**
     * 사용자가 선택한 지역을 세션에 저장하고, 해당 지역의 ID를 반환하는 메서드
     *
     * @param requestDto 클라이언트로부터 받은 지역 정보 DTO
     * @param session    현재 HTTP 세션
     * @return 세션에 저장된 지역의 PK (ID)
     */
    public Long setForumArea(ForumAreaRequestDto requestDto, HttpSession session) {

        String newLocationAddress = requestDto.getLocation();
        ForumAreaEntity area = forumAreaRepository.findByAddress(newLocationAddress)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역 정보입니다."));
        // 세션에 지역 주소(address)를 저장
        session.setAttribute("selectedForumAreaAddress", area.getAddress());
        // 지역의 PK를 반환하여 컨트롤러가 리다이렉트 URL을 구성
        return area.getId();
    }

    /**
     * 주소 문자열을 사용하여 지역의 PK (ID)를 조회합니다.
     *
     * @param address 조회할 지역의 주소 문자열
     * @return 해당 지역의 PK (ID)
     */
    public Long getAreaKeyByAddress(String address) {
        ForumAreaEntity area = forumAreaRepository.findByAddress(address)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 지역 정보입니다."));
        return area.getId();
    }

    /**
     * 모든 포럼 지역 목록을 조회합니다.
     *
     * @return 모든 지역 정보 DTO 리스트
     */
    public List<ForumAreaResponseDto> getAllAreas() {
        return forumAreaRepository.findAll().stream()
                .map(ForumAreaResponseDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 지정된 지역에 해당하는 포럼 게시물 목록을 조회합니다.
     * 댓글 및 좋아요 정보를 포함합니다.
     *
     * @param locationKey   지역 주소 또는 ID (String 형태)
     * @param currentUserId 현재 로그인한 사용자 ID
     * @return 해당 지역의 포럼 게시물 목록 DTO 리스트
     */
    public List<ForumPostListResponseDto> getForumPostsByLocation(String locationKey, Long currentUserId) {

        // final 변수에 currentUserId 값을 담아 고정
        final Long finalCurrentUserId = currentUserId;

        ForumAreaEntity area;
        try {
            Long locationId = Long.parseLong(locationKey);
            area = forumAreaRepository.findById(locationId)
                    .orElseThrow(() -> new EntityNotFoundException("유효하지 않은 지역 ID입니다."));
        } catch (NumberFormatException e) {
            area = forumAreaRepository.findByAddress(locationKey)
                    .orElseThrow(() -> new EntityNotFoundException("유효하지 않은 지역 주소입니다."));
        }

        List<ForumEntity> forumEntities = forumRepository.findByLocation(area);

        // Entity를 DTO로 변환하면서 댓글 정보를 추가합니다.
        return forumEntities.stream()
                .map(forumEntity -> {
                    int totalComments = forumCommentRepository.countByForumId(forumEntity.getId());
                    int totalLikes = forumLikeRepository.countByForumId(forumEntity.getId());

                    LocalDateTime createdAt = forumEntity.getCreatedAt();

                    boolean isLiked = false; // 기본값
                    List<String> recentLikerPhotos = forumLikeRepository.findTop3ByForumIdOrderByCreatedAtDesc(forumEntity.getId())
                            .stream()
                            .map(like -> like.getLiker().getProfilePhotoUrl())
                            .collect(Collectors.toList());

                    boolean isAuthor = false;

                    // 현재 사용자가 로그인한 상태일 경우에만 좋아요 여부를 확인
                    if (finalCurrentUserId != null) {
                        isLiked = forumLikeRepository.findByForumIdAndLikerId(forumEntity.getId(), finalCurrentUserId).isPresent();
                        isAuthor = forumEntity.getWriter().getId().equals(finalCurrentUserId);
                    }

                    return new ForumPostListResponseDto(
                            forumEntity,
                            totalComments,
                            createdAt,
                            totalLikes,
                            isLiked,
                            recentLikerPhotos,
                            isAuthor
                    );
                })
                .collect(Collectors.toList());
    }

    public List<ForumPostListResponseDto> getForumPostsByLocation(String locationKey, Long currentUserId) {
        ForumAreaEntity area;
        try {
            Long locationId = Long.parseLong(locationKey);
            area = forumAreaRepository.findById(locationId)
                    .orElseThrow(() -> new EntityNotFoundException("유효하지 않은 지역 ID입니다."));
        } catch (NumberFormatException e) {
            area = forumAreaRepository.findByAddress(locationKey)
                    .orElseThrow(() -> new EntityNotFoundException("유효하지 않은 지역 주소입니다."));
        }
        List<ForumEntity> forumEntities = forumRepository.findByLocation(area);
        return forumEntities.stream()
                .map(forumEntity -> convertToPostListDto(forumEntity, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자가 작성한 Forum 게시물 목록을 페이지 단위로 조회
     */
    @Transactional(readOnly = true)
    public Page<ForumPostSummaryDto> getMyForums(String userEmail, Pageable pageable) {
        UserInfoEntity user = userInfoRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Page<ForumEntity> forumsPage = forumRepository.findByWriterOrderByIdDesc(user, pageable);

        return forumsPage.map(forum -> {
            String imageUrl = forum.getImages().isEmpty()
                    ? "https://placehold.co/400x400/E2E8F0/4A5568?text=No+Image" // 이미지가 없을 경우 기본 이미지
                    : forum.getImages().get(0).getImgUrl(); // 첫 번째 이미지를 대표 이미지로 사용

            return ForumPostSummaryDto.builder()
                    .id(forum.getId())
                    .imageUrl(imageUrl)
                    .location(forum.getLocation().getAddress())
                    .build();
        });
    }

    @Transactional(readOnly = true)
    public Page<ForumPostSummaryDto> getMyForums(String userEmail, Pageable pageable) {
        UserInfoEntity user = userInfoRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Page<ForumEntity> forumsPage = forumRepository.findByWriterForProfile(user, pageable);
        return forumsPage.map(forum -> {
            String imageUrl = forum.getImages().isEmpty() ? null : forum.getImages().get(0).getImgUrl();
            return ForumPostSummaryDto.builder()
                    .id(forum.getId())
                    .imageUrl(imageUrl)
                    .location(forum.getLocation() != null ? forum.getLocation().getAddress() : "위치 정보 없음")
                    .contentsText(forum.getContentsText())
                    .build();
        });
    }

    @Transactional(readOnly = true)
    public Page<ForumPostSummaryDto> getForumsByNickname(String nickname, Pageable pageable) {
        UserInfoEntity user = userInfoRepository.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Page<ForumEntity> forumsPage = forumRepository.findByWriterOrderByIdDesc(user, pageable);

        return forumsPage.map(forum -> {
            String imageUrl = forum.getImages().isEmpty()
                    ? "https://placehold.co/400x400/E2E8F0/4A5568?text=No+Image" // 이미지가 없을 경우 기본 이미지
                    : forum.getImages().get(0).getImgUrl(); // 첫 번째 이미지를 대표 이미지로 사용

            return ForumPostSummaryDto.builder()
                    .id(forum.getId())
                    .imageUrl(imageUrl)
                    .location(forum.getLocation().getAddress())
                    .build();
        });
    }

    @Transactional(readOnly = true)
    public Page<ForumPostSummaryDto> getForumsByNickname(String nickname, Pageable pageable) {
        UserInfoEntity user = userInfoRepository.findByNickname(nickname)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Page<ForumEntity> forumsPage = forumRepository.findByWriterForProfile(user, pageable);
        return forumsPage.map(forum -> {
            String imageUrl = forum.getImages().isEmpty() ? null : forum.getImages().get(0).getImgUrl();
            return ForumPostSummaryDto.builder()
                    .id(forum.getId())
                    .imageUrl(imageUrl)
                    .location(forum.getLocation() != null ? forum.getLocation().getAddress() : "위치 정보 없음")
                    .contentsText(forum.getContentsText())
                    .build();
        });
    }

    @Transactional(readOnly = true)
    public List<ForumPostListResponseDto> getAllForumPostsForFeed(Long currentUserId) {
        List<ForumEntity> forumEntities = forumRepository.findAllByOrderByCreatedAtDesc();
        return forumEntities.stream()
                .map(forumEntity -> convertToPostListDto(forumEntity, currentUserId))
                .collect(Collectors.toList());
    }

    private ForumPostListResponseDto convertToPostListDto(ForumEntity forumEntity, Long currentUserId) {
        int totalComments = forumCommentRepository.countByForumId(forumEntity.getId());
        int totalLikes = forumLikeRepository.countByForumId(forumEntity.getId());
        List<String> recentLikerPhotos = forumLikeRepository.findTop3ByForumIdOrderByCreatedAtDesc(forumEntity.getId())
                .stream()
                .map(like -> like.getLiker() != null ? like.getLiker().getProfilePhotoUrl() : null)
                .collect(Collectors.toList());
        boolean isLiked = false;
        boolean isAuthor = false;
        if (currentUserId != null && forumEntity.getWriter() != null) {
            isLiked = forumLikeRepository.findByForumIdAndLikerId(forumEntity.getId(), currentUserId).isPresent();
            isAuthor = forumEntity.getWriter().getId().equals(currentUserId);
        }
        return new ForumPostListResponseDto(
                forumEntity,
                totalComments,
                forumEntity.getCreatedAt(),
                totalLikes,
                isLiked,
                recentLikerPhotos,
                isAuthor
        );
    }

    @Transactional(readOnly = true)
    public ForumDetailViewDto getForumPostDetail(Long forumId, Long currentUserId) {
        ForumEntity forum = forumRepository.findByIdWithDetails(forumId)
                .orElseThrow(() -> new EntityNotFoundException("해당 게시물을 찾을 수 없습니다. id=" + forumId));

        int totalLikes = forumLikeRepository.countByForumId(forumId);
        boolean isLiked = false;
        if (currentUserId != null) {
            isLiked = forumLikeRepository.findByForumIdAndLikerId(forumId, currentUserId).isPresent();
        }

        List<ForumCommentEntity> comments = forumCommentRepository.findAllByForumIdOrderByCreatedAtAsc(forumId);
        List<ForumCommentDto> commentDtos = comments.stream()
                .map(comment -> new ForumCommentDto(comment, currentUserId))
                .collect(Collectors.toList());

        return new ForumDetailViewDto(forum, totalLikes, isLiked, commentDtos, currentUserId);
    }
}