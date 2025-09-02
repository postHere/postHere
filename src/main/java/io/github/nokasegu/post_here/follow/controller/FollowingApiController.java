package io.github.nokasegu.post_here.follow.controller;

import io.github.nokasegu.post_here.follow.dto.FollowingStatusRequestDto;
import io.github.nokasegu.post_here.follow.dto.FollowingStatusResponseDto;
import io.github.nokasegu.post_here.follow.dto.UserBriefResponseDto;
import io.github.nokasegu.post_here.follow.service.FollowingService;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friends")
public class FollowingApiController {

    private final FollowingService followingService;
    private final UserInfoRepository userInfoRepository;

    /**
     * 현재 사용자 PK 해석 규칙
     * 1) 개발용 헤더 X-USER-ID(숫자) 있으면 최우선 사용
     * 2) principal.getName()이 숫자면 그대로 PK
     * 3) 문자열이면 loginId로 간주 후 조회 → PK
     * <p>
     * 🚀 새 ERD(이메일 기준)로 전환하면 아래 3) 분기를 이 한 줄로 바꾸세요:
     * Long id = userInfoRepository.findByEmail(name)
     * .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자(email) 없음: " + name))
     * .getId();
     * return id;
     */
    private Long resolveUserId(Principal principal, Long overrideHeader) {
        if (overrideHeader != null) return overrideHeader;

        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 필요");
        }
        String name = principal.getName();
        try {
            return Long.valueOf(name); // 숫자면 PK로 간주
        } catch (NumberFormatException ignore) {
            // 로그인 아이디로 조회 (현재 DB/코드 호환용)  ※ 메서드명은 findByLoginId 지만 쿼리는 email 기준
            UserInfoEntity user = userInfoRepository.findByLoginId(name);
            if (user == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자(loginId) 없음: " + name);
            return user.getId();
        }
    }

    // ===== Followers (로그인 사용자 기준)
    @GetMapping("/followers")
    public Page<UserBriefResponseDto> followers(
            Principal principal,
            @RequestHeader(name = "X-USER-ID", required = false) Long userHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long me = resolveUserId(principal, userHeader);
        return followingService
                .getFollowers(me, PageRequest.of(page, size))
                .map(UserBriefResponseDto::convertToDto);
    }

    // (개발/테스트용) 임의 사용자 기준
    @GetMapping("/followers/{userId}")
    public Page<UserBriefResponseDto> followersById(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return followingService
                .getFollowers(userId, PageRequest.of(page, size))
                .map(UserBriefResponseDto::convertToDto);
    }

    // ===== Followings (로그인 사용자 기준)
    @GetMapping("/followings")
    public Page<UserBriefResponseDto> followings(
            Principal principal,
            @RequestHeader(name = "X-USER-ID", required = false) Long userHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long me = resolveUserId(principal, userHeader);
        return followingService
                .getFollowings(me, PageRequest.of(page, size))
                .map(UserBriefResponseDto::convertToDto);
    }

    // (개발/테스트용) 임의 사용자 기준
    @GetMapping("/followings/{userId}")
    public Page<UserBriefResponseDto> followingsById(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return followingService
                .getFollowings(userId, PageRequest.of(page, size))
                .map(UserBriefResponseDto::convertToDto);
    }

    // ===== 배치 상태: ids 중 내가 팔로우한 대상은 true
    @PostMapping("/status")
    public FollowingStatusResponseDto status(
            Principal principal,
            @RequestHeader(name = "X-USER-ID", required = false) Long userHeader,
            @RequestBody FollowingStatusRequestDto req
    ) {
        Long me = resolveUserId(principal, userHeader);
        return FollowingStatusResponseDto.from(
                followingService.getFollowingStatus(me, req.getIds())
        );
    }

    // ===== ✅ 닉네임 검색 (로그인 사용자 제외)
    @GetMapping("/search")
    public Page<UserBriefResponseDto> search(
            Principal principal,
            @RequestHeader(name = "X-USER-ID", required = false) Long userHeader,
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long me = resolveUserId(principal, userHeader);
        return followingService
                .getUsersByNickname(me, keyword, PageRequest.of(page, size))
                .map(UserBriefResponseDto::convertToDto);
    }
}
