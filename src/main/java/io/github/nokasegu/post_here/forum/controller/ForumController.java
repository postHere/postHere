package io.github.nokasegu.post_here.forum.controller;

import io.github.nokasegu.post_here.forum.service.ForumService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ForumController {

    private final ForumService forumService;
}
