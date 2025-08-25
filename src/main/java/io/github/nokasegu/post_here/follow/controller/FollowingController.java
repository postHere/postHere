package io.github.nokasegu.post_here.follow.controller;

import io.github.nokasegu.post_here.follow.service.FollowingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class FollowingController {

    private final FollowingService followingService;

}
