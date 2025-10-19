package io.github.nokasegu.post_here.find.controller;

import io.github.nokasegu.post_here.common.security.CustomUserDetails;
import io.github.nokasegu.post_here.find.domain.FindEntity;
import io.github.nokasegu.post_here.find.dto.FindDetailViewDto;
import io.github.nokasegu.post_here.find.service.FindService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class FindController {

    private final FindService findService;

    //구글 API 키 삽입
    @Value("${google.maps.api.key:}")
    private String googleMapsApiKey;

    @GetMapping("/find")
    public String find() {
        return "find/find-write";
    }

    @GetMapping("/find/{no}")
    public String findUpdate(@PathVariable Long no, Model model) {

        FindEntity find = findService.getFindById(no);
        model.addAttribute("find_no", find.getId());
        model.addAttribute("find_url", find.getContentOverwriteUrl());

        return "find/find-overwrite";
    }

    @GetMapping("/find/on-map")
    public String findOnMap(Model model) {
        model.addAttribute("googleMapsApiKey", googleMapsApiKey);
        return "find/find-on-map";
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
