package io.github.nokasegu.post_here.find.controller;

import io.github.nokasegu.post_here.common.security.CustomUserDetails;
import io.github.nokasegu.post_here.find.domain.FindEntity;
import io.github.nokasegu.post_here.find.dto.FindViewerDto;
import io.github.nokasegu.post_here.find.service.FindService;
import io.github.nokasegu.post_here.userInfo.service.UserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
@Slf4j
public class FindController {

    private final FindService findService;
    private final UserInfoService userInfoService;

    //구글 API 키 삽입
    @Value("${google.maps.api.key:}")
    private String googleMapsApiKey;

    @GetMapping("/find")
    public String find() {
        return "/find/find-write";
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
        return "/find/find-on-map";
    }

    // FindController.java
    @GetMapping("/find/original/{startFindId}")
    public String getFindOriginalPage(@PathVariable Long startFindId,
                                      @AuthenticationPrincipal CustomUserDetails userDetails,
                                      Model model) {
        Long currentUserId = userDetails.getUserInfo().getId();
        FindViewerDto viewerData = findService.getFindsForViewer(startFindId, currentUserId);
        model.addAttribute("viewerData", viewerData);
        return "find/find-viewer";
    }
}
