package io.github.nokasegu.post_here.forum.service;

import io.github.nokasegu.post_here.forum.domain.ForumAreaEntity;
import io.github.nokasegu.post_here.forum.domain.ForumEntity;
import io.github.nokasegu.post_here.forum.dto.ForumCreateRequestDto;
import io.github.nokasegu.post_here.forum.dto.ForumCreateResponseDto;
import io.github.nokasegu.post_here.forum.dto.ForumPostListResponseDto;
import io.github.nokasegu.post_here.forum.repository.ForumAreaRepository;
import io.github.nokasegu.post_here.forum.repository.ForumRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumService {

    private final ForumRepository forumRepository;
    private final UserInfoRepository userInfoRepository;
    private final ForumAreaRepository forumAreaRepository;
    private final ForumImageService forumImageService;

    @Transactional
    public ForumCreateResponseDto createForum(ForumCreateRequestDto requestDto) throws IOException {

        // 1. 서비스 내부에서 DTO에 담긴 이메일로 유저를 조회합니다.
        String userEmail = requestDto.getUserEmail();
        UserInfoEntity writer = userInfoRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));

        ForumAreaEntity area = forumAreaRepository.findById(requestDto.getLocation())
                .orElseThrow(() -> new EntityNotFoundException("유효하지 않은 지역입니다"));

        // 2. 텍스트, 위치, 음악 정보 등을 담은 ForumEntity를 생성하고 저장합니다.
        ForumEntity forum = ForumEntity.builder()
                .writer(writer)
                .location(area)
                .contentsText(requestDto.getContent())
                .musicApiUrl(requestDto.getSpotifyTrackId())
                .build();
        ForumEntity savedForum = forumRepository.save(forum);

        // 3. 이미지 저장 로직을 ForumImageService에 위임합니다.
        forumImageService.saveImages(savedForum, requestDto.getImageUrls());

        // 4. 컨트롤러에 전달할 응답 DTO를 생성하여 반환합니다.
        return new ForumCreateResponseDto(savedForum.getId());
    }

    /**
     * 지정된 지역에 해당하는 포럼 게시물 목록을 조회합니다.
     *
     * @param locationKey 지역 주소 또는 ID (String 형태)
     * @return 해당 지역의 포럼 게시물 목록 DTO 리스트
     */
    public List<ForumPostListResponseDto> getForumPostsByLocation(String locationKey) {
        ForumAreaEntity area;

        try {
            // key가 Long 타입인지 확인하여 ID로 지역을 조회합니다.
            Long locationId = Long.parseLong(locationKey);
            area = forumAreaRepository.findById(locationId)
                    .orElseThrow(() -> new EntityNotFoundException("유효하지 않은 지역 ID입니다."));
        } catch (NumberFormatException e) {
            // 숫자가 아니면 key를 주소로 간주하고 지역을 조회합니다.
            area = forumAreaRepository.findByAddress(locationKey)
                    .orElseThrow(() -> new EntityNotFoundException("유효하지 않은 지역 주소입니다."));
        }

        List<ForumEntity> forumEntities = forumRepository.findByLocation(area);

        // Entity를 DTO로 변환하여 반환합니다.
        return forumEntities.stream()
                .map(ForumPostListResponseDto::new)
                .collect(Collectors.toList());
    }
}