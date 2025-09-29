package io.github.nokasegu.post_here.find.controller;

import io.github.nokasegu.post_here.find.domain.FindEntity;
import io.github.nokasegu.post_here.find.service.FindService;
import io.github.nokasegu.post_here.userInfo.service.UserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @GetMapping("/find")
    public String find() {
        return "/find/find-write";
    }

    @GetMapping("/find/{no}/edit")
    public String findUpdate(@PathVariable Long no, Model model) {

        FindEntity find = findService.getFindById(no);
        model.addAttribute("find_no", find.getId());
        model.addAttribute("find_url", find.getContentOverwriteUrl());

        return "find/find-overwrite";
    }
}
