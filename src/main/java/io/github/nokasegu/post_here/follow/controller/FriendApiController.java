package io.github.nokasegu.post_here.follow.controller;

import io.github.nokasegu.post_here.follow.dto.*;
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
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/friend")
public class FriendApiController {

    private final FollowingService followingService;
    private final UserInfoRepository userInfoRepository;

    /**
     * 현재 로그인 사용자 ID 해석
     */
    private Long resolveUserId(Principal principal,
                               @RequestHeader(name = "X-USER-ID", required = false) Long overrideHeader) {
        if (overrideHeader != null) return overrideHeader;
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 필요");
        }
        String name = principal.getName();
        try {
            return Long.valueOf(name);
        } catch (NumberFormatException ignore) {
            UserInfoEntity user = userInfoRepository.findByLoginId(name);
            if (user == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자(loginId) 없음: " + name);
            return user.getId();
        }
    }

    // ===== 스펙: 팔로워 목록 조회 (나를 팔로우하는 사람) — GET /friend/followlist
    @GetMapping("/followlist")
    public Page<UserBriefResponseDto> followerList(
            Principal principal,
            @RequestHeader(name = "X-USER-ID", required = false) Long userHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long me = resolveUserId(principal, userHeader);
        return followingService.getFollowers(me, PageRequest.of(page, size))
                .map(UserBriefResponseDto::convertToDto);
    }

    // ===== 스펙: 팔로잉 목록 조회 (내가 팔로우하는 사람) — GET /friend/followinglist
    @GetMapping("/followinglist")
    public Page<UserBriefResponseDto> followingList(
            Principal principal,
            @RequestHeader(name = "X-USER-ID", required = false) Long userHeader,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long me = resolveUserId(principal, userHeader);
        return followingService.getFollowings(me, PageRequest.of(page, size))
                .map(UserBriefResponseDto::convertToDto);
    }

    // ===== (화면용) 상태 배치 조회 — POST /friend/status  (ids: Long[])
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

    // ===== (선택) 닉네임 검색 — GET /friend/search?q=...
    @GetMapping("/search")
    public Page<UserBriefResponseDto> search(
            Principal principal,
            @RequestHeader(name = "X-USER-ID", required = false) Long userHeader,
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long me = resolveUserId(principal, userHeader);
        return followingService.getUsersByNickname(me, keyword, PageRequest.of(page, size))
                .map(UserBriefResponseDto::convertToDto);
    }

    // ===== 스펙: 친구 추가(팔로우) — POST /friend/addfollowing
    // - body: { "userId": 123 } 또는 query: ?userId=123 (둘 다 허용)
    @PostMapping("/addfollowing")
    public FollowActionResponseDto addFollowing(
            Principal principal,
            @RequestHeader(name = "X-USER-ID", required = false) Long userHeader,
            @RequestParam(name = "userId", required = false) Long userIdParam,
            @RequestBody(required = false) FollowTargetRequestDto body
    ) {
        Long me = resolveUserId(principal, userHeader);
        Long targetId = Optional.ofNullable(userIdParam)
                .orElseGet(() -> body != null ? body.getUserId() : null);
        if (targetId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId 필요");
        followingService.follow(me, targetId); // idempotent
        return new FollowActionResponseDto(targetId, true);
    }

    // ===== 스펙: 친구 삭제(언팔로우) — DELETE /friend/unfollowing
    // - body: { "userId": 123 } 또는 query: ?userId=123 (둘 다 허용)
    @DeleteMapping("/unfollowing")
    public FollowActionResponseDto unfollowing(
            Principal principal,
            @RequestHeader(name = "X-USER-ID", required = false) Long userHeader,
            @RequestParam(name = "userId", required = false) Long userIdParam,
            @RequestBody(required = false) FollowTargetRequestDto body
    ) {
        Long me = resolveUserId(principal, userHeader);
        Long targetId = Optional.ofNullable(userIdParam)
                .orElseGet(() -> body != null ? body.getUserId() : null);
        if (targetId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId 필요");
        followingService.unfollow(me, targetId); // idempotent
        return new FollowActionResponseDto(targetId, false);
    }
}
