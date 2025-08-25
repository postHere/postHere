package io.github.nokasegu.post_here.forum.controller;

import io.github.nokasegu.post_here.forum.service.ForumLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ForumLikeController {

    private final ForumLikeService forumLikeService;
}
