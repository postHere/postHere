package io.github.nokasegu.post_here.userInfo.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    // 스펙: GET /profile/{profile_id}
    @GetMapping("/profile/{userId}")
    public String profile(@PathVariable Long userId) {
        // 약식: 데이터는 AJAX로 /profile/info/{id}에서 가져옴
        // 프로필 페이지가 아직 없는데 팔로우/언팔로우 기능 구현되나 보려고 만들었슴(정민)
        return "userInfo/profile";
    }
}
