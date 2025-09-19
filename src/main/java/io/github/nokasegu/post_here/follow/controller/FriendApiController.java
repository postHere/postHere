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
/**
 * [Flow (A) 트리거: 팔로우]
 * - addFollowing(...) 엔드포인트가 로그인 사용자(me)와 대상(userId)를 받아
 *   FollowingService.follow(me, userId)를 호출한다.
 * - Service 계층은 중복 팔로우 여부 검사 후 INSERT를 수행하고,
 *   이어서 NotificationService.createFollowAndPush(...)로 알림 생성/푸시를 위임한다.
 *
 * 역할 경계
 * - Controller: 요청 파라미터/인증(Principal) 수집 및 Service 호출
 * - Service: 비즈니스 로직(검증·INSERT·알림 트리거)
 */
public class FriendApiController {

    private final FollowingService followingService;
    private final UserInfoRepository userInfoRepository;

    /**
     * 현재 로그인 사용자 ID 해석
     * - 프로젝트 전반 정책과 통일: 이메일 우선 → 실패 시 숫자(PK) → (레거시) loginId
     */
    private Long resolveUserId(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 필요");
        }
        String name = principal.getName();

        // 1) 이메일 기반
        UserInfoEntity byEmail = userInfoRepository.findByEmail(name).orElse(null);
        if (byEmail != null) return byEmail.getId();

        // 2) 숫자 문자열이면 PK 조회
        try {
            long asId = Long.parseLong(name);
            return userInfoRepository.findById(asId)
                    .map(UserInfoEntity::getId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자 없음"));
        } catch (NumberFormatException ignored) {
            // pass
        }

        // 3) (레거시) loginId 메서드 — 현재 쿼리는 email 기준으로 매핑됨
        UserInfoEntity byLoginId = userInfoRepository.findByLoginId(name);
        if (byLoginId != null) return byLoginId.getId();

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증 주체를 해석할 수 없음");
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

    // [Flow (A)] 팔로우 요청 진입점
    // - (me, userId) 수신 → FollowingService.follow(me, userId) 호출
    // - 이후 Service에서 중복 검사/INSERT → NotificationService.createFollowAndPush(...) 트리거

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

    // [Flow (A)] 언팔로우 요청 진입점
    // - (me, userId) 수신 → FollowingService.unfollow(me, userId) 호출 (멱등)

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
