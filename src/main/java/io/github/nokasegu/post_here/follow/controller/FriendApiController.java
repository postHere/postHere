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

@RestController
@RequiredArgsConstructor
@RequestMapping("/friend")
public class FriendApiController {

    private final FollowingService followingService;
    private final UserInfoRepository userInfoRepository;

    /**
     * 현재 로그인 사용자 ID만 사용(커스텀 헤더 제거)
     */
    private Long resolveUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 필요");
        }
        String name = principal.getName();
        try {
            return Long.valueOf(name); // 숫자면 PK
        } catch (NumberFormatException ignore) {
            UserInfoEntity user = userInfoRepository.findByLoginId(name);
            if (user == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자(loginId) 없음: " + name);
            return user.getId();
        }
    }

    /**
     * 팔로워 목록(나를 팔로우)
     */
    @GetMapping("/followlist")
    public Page<UserBriefResponseDto> followerList(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long me = resolveUserId(principal);
        return followingService.getFollowers(me, PageRequest.of(page, size))
                .map(UserBriefResponseDto::convertToDto);
    }

    /**
     * 팔로잉 목록(내가 팔로우)
     */
    @GetMapping("/followinglist")
    public Page<UserBriefResponseDto> followingList(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long me = resolveUserId(principal);
        return followingService.getFollowings(me, PageRequest.of(page, size))
                .map(UserBriefResponseDto::convertToDto);
    }

    /**
     * 상태 배치 조회(화면용)
     */
    @PostMapping("/status")
    public FollowingStatusResponseDto status(
            Principal principal,
            @RequestBody FollowingStatusRequestDto req
    ) {
        Long me = resolveUserId(principal);
        return FollowingStatusResponseDto.from(
                followingService.getFollowingStatus(me, req.getIds())
        );
    }

    /**
     * 닉네임 검색(로그인 사용자 제외)
     */
    @GetMapping("/search")
    public Page<UserBriefResponseDto> search(
            Principal principal,
            @RequestParam("q") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long me = resolveUserId(principal);
        return followingService.getUsersByNickname(me, keyword, PageRequest.of(page, size))
                .map(UserBriefResponseDto::convertToDto);
    }

    /**
     * 친구 추가(팔로우) — JSON body만 허용(쿼리스트링 금지)
     */
    @PostMapping("/addfollowing")
    public FollowActionResponseDto addFollowing(
            Principal principal,
            @RequestBody FollowTargetRequestDto body
    ) {
        Long me = resolveUserId(principal);
        if (body == null || body.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId 필요");
        }
        followingService.follow(me, body.getUserId()); // idempotent
        return new FollowActionResponseDto(body.getUserId(), true);
    }

    /**
     * 친구 삭제(언팔로우) — JSON body만 허용(쿼리스트링 금지)
     */
    @DeleteMapping("/unfollowing")
    public FollowActionResponseDto unfollowing(
            Principal principal,
            @RequestBody FollowTargetRequestDto body
    ) {
        Long me = resolveUserId(principal);
        if (body == null || body.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId 필요");
        }
        followingService.unfollow(me, body.getUserId()); // idempotent
        return new FollowActionResponseDto(body.getUserId(), false);

    }
}
