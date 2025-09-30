package io.github.nokasegu.post_here.find.controller;

import io.github.nokasegu.post_here.common.dto.WrapperDTO;
import io.github.nokasegu.post_here.common.exception.Code;
import io.github.nokasegu.post_here.common.security.CustomUserDetails;
import io.github.nokasegu.post_here.find.dto.FindDetailViewDto;
import io.github.nokasegu.post_here.find.dto.FindNearbyResponseDto;
import io.github.nokasegu.post_here.find.service.FindService;
import io.github.nokasegu.post_here.location.dto.LocationRequestDto;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import io.github.nokasegu.post_here.userInfo.service.UserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class FindController {

    private final FindService findService;
    private final UserInfoService userInfoService;

    @GetMapping("/find")
    public String findController(Model model) {
        log.debug("testtttttttttttttt");
        return "/find/find-write";
    }

    @GetMapping("/around-finds")
    public WrapperDTO<List<FindNearbyResponseDto>> whereAmI(@RequestBody LocationRequestDto location) {

        UserInfoEntity user = userInfoService.getUserInfoByEmail(location.getUser());
        List<FindNearbyResponseDto> findList = findService.getFindsInArea(location.getLng(), location.getLat(), user.getId());

        return WrapperDTO.<List<FindNearbyResponseDto>>builder()
                .status(Code.OK.getCode())
                .message(Code.OK.getValue())
                .data(findList)
                .build();
    }

    @GetMapping("/find/feed/{startFindId}")
    public String getFindFeedPage(@PathVariable Long startFindId,
                                  @AuthenticationPrincipal CustomUserDetails userDetails,
                                  Model model) {
        // 1. 현재 로그인한 사용자의 ID를 확인합니다. (비로그인 시 null)
        Long currentUserId = (userDetails != null) ? userDetails.getUserInfo().getId() : null;

        // 2. 서비스 로직을 호출하여 피드에 필요한 모든 데이터를 가져옵니다.
        List<FindDetailViewDto> posts = findService.getFindFeed(startFindId, currentUserId);

        // 3. 서비스로부터 받은 데이터를 "posts"라는 이름으로 HTML에게 전달합니다.
        model.addAttribute("posts", posts);

        // 4. "resources/templates/find/feed.html" 파일을 찾아 화면에 보여줍니다.
        return "find/find-viewer";
    }
}
