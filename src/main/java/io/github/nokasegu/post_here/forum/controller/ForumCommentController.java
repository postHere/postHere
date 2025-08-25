package io.github.nokasegu.post_here.forum.controller;

import io.github.nokasegu.post_here.forum.service.ForumCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ForumCommentController {

    private final ForumCommentService forumCommentService;
}
