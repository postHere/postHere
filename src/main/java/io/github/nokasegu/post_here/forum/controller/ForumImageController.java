package io.github.nokasegu.post_here.forum.controller;

import io.github.nokasegu.post_here.forum.service.ForumImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ForumImageController {

    private final ForumImageService forumImageService;
}
