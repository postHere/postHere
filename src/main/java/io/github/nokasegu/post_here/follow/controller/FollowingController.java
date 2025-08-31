package io.github.nokasegu.post_here.follow.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class FollowingController {

    // 친구 목록 단일 페이지 (탭으로 Follower/Following/Search 전환)
    @GetMapping("/friends")
    public String friendsPage() {
        // src/main/resources/templates/follow/friends.html
        return "follow/friends";
    }
}
