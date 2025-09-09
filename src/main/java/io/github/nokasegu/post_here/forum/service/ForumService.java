package io.github.nokasegu.post_here.forum.service;

import io.github.nokasegu.post_here.forum.domain.ForumAreaEntity;
import io.github.nokasegu.post_here.forum.domain.ForumEntity;
import io.github.nokasegu.post_here.forum.dto.ForumCreateRequestDto;
import io.github.nokasegu.post_here.forum.dto.ForumCreateResponseDto;
import io.github.nokasegu.post_here.forum.repository.ForumAreaRepository;
import io.github.nokasegu.post_here.forum.repository.ForumRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

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
}