package io.github.nokasegu.post_here.forum.service;

import io.github.nokasegu.post_here.forum.domain.ForumAreaEntity;
import io.github.nokasegu.post_here.forum.domain.ForumEntity;
import io.github.nokasegu.post_here.forum.dto.*;
import io.github.nokasegu.post_here.forum.repository.ForumAreaRepository;
import io.github.nokasegu.post_here.forum.repository.ForumCommentRepository;
import io.github.nokasegu.post_here.forum.repository.ForumRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumService {

    private final ForumRepository forumRepository;
    private final UserInfoRepository userInfoRepository;
    private final ForumAreaRepository forumAreaRepository;
    private final ForumImageService forumImageService;
    private final ForumCommentRepository forumCommentRepository;

    /**
     * 포럼 게시글을 생성하고 저장
     *
     * @param requestDto 게시글 생성 요청 DTO
     * @return 생성된 게시글의 ID가 담긴 응답 DTO
     */
    @Transactional
    public ForumCreateResponseDto createForum(ForumCreateRequestDto requestDto) throws IOException {

        // DTO에서 사용자 이메일을 가져와 유저를 조회
        UserInfoEntity writer = userInfoRepository.findByEmail(requestDto.getUserEmail())
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));

        // DTO의 지역 ID로 ForumAreaEntity를 조회
        ForumAreaEntity area = forumAreaRepository.findById(requestDto.getLocation())
                .orElseThrow(() -> new EntityNotFoundException("유효하지 않은 지역입니다"));

        // ForumEntity를 생성하고, 조회된 Entity를 location 필드에 연결
        ForumEntity forum = ForumEntity.builder()
                .writer(writer)
                .location(area)
                .contentsText(requestDto.getContent())
                .musicApiUrl(requestDto.getSpotifyTrackId())
                .createdAt(LocalDateTime.now())
                .build();
        ForumEntity savedForum = forumRepository.save(forum);

        // 이미지 저장 로직을 ForumImageService에 위임
        forumImageService.saveImages(savedForum, requestDto.getImageUrls());

        // 컨트롤러에 전달할 응답 DTO를 생성하여 반환
        return new ForumCreateResponseDto(savedForum.getId());
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
     * 댓글 정보를 포함합니다.
     *
     * @param locationKey 지역 주소 또는 ID (String 형태)
     * @return 해당 지역의 포럼 게시물 목록 DTO 리스트
     */
    public List<ForumPostListResponseDto> getForumPostsByLocation(String locationKey) {
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
                    return new ForumPostListResponseDto(
                            forumEntity,
                            totalComments,
                            forumEntity.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());
    }
}