package io.github.nokasegu.post_here.find.controller;

import io.github.nokasegu.post_here.common.dto.WrapperDTO;
import io.github.nokasegu.post_here.common.exception.Code;
import io.github.nokasegu.post_here.find.dto.FindNearbyResponseDto;
import io.github.nokasegu.post_here.find.dto.FindPostSummaryDto;
import io.github.nokasegu.post_here.find.dto.FindRequestDto;
import io.github.nokasegu.post_here.find.service.FindService;
import io.github.nokasegu.post_here.location.dto.LocationRequestDto;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.service.UserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class FindAPIController {

    private final FindService findService;
    private final UserInfoService userInfoService;

    // 미사용으로 추정
    @GetMapping("/finds/my-posts")
    public ResponseEntity<Page<FindPostSummaryDto>> getMyFinds(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 4) Pageable pageable) {

        if (userDetails == null) return ResponseEntity.status(401).build();

        Page<FindPostSummaryDto> result = findService.getMyFinds(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/profile/findlist/{nickname}")
    public ResponseEntity<Page<FindPostSummaryDto>> getFindsForUser(
            @PathVariable String nickname,
            @PageableDefault(size = 4) Pageable pageable) {

        Page<FindPostSummaryDto> result = findService.getFindsByNickname(nickname, pageable);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/find")
    public void find(@ModelAttribute FindRequestDto findRequestDto, @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        log.info("findRequest의 이미지 파일 {} ", findRequestDto.getContent_capture());
        findService.saveFind(findRequestDto, userDetails.getUsername());
    }

    @DeleteMapping("/find/{no}")
    public void delete(@PathVariable Long no) {
        findService.deleteFind(no);
    }

    @PostMapping("/find/{no}")
    public void update(@PathVariable Long no, @RequestBody FindRequestDto findRequestDto) throws IOException {

        findService.updateFind(no, findRequestDto.getContent_capture());
    }

    @PostMapping("/find/around")
    public WrapperDTO<List<FindNearbyResponseDto>> whereAmI(@RequestBody LocationRequestDto location, @AuthenticationPrincipal UserDetails userDetails) {

        UserInfoEntity user = userInfoService.getUserInfoByEmail(userDetails.getUsername());
        List<FindNearbyResponseDto> findList = findService.getFindsInArea(location.getLng(), location.getLat(), user.getId());

        return WrapperDTO.<List<FindNearbyResponseDto>>builder()
                .status(Code.OK.getCode())
                .message(Code.OK.getValue())
                .data(findList)
                .build();
    }
}