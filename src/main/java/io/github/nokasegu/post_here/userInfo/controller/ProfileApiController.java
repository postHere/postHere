package io.github.nokasegu.post_here.userInfo.controller;

// 프로필 페이지가 아직 없는데 팔로우/언팔로우 기능 구현되나 보려고 만들었슴(정민)

import io.github.nokasegu.post_here.follow.dto.UserBriefResponseDto;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/profile")
public class ProfileApiController {

    private final UserInfoRepository userInfoRepository;


    // 스펙: GET (AJAX) /profile/info/{profile_id}
    @GetMapping("/info/{userId}")
    public UserBriefResponseDto info(@PathVariable Long userId) {
        UserInfoEntity e = userInfoRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 없음: " + userId));
        return UserBriefResponseDto.convertToDto(e);
    }
}
