package io.github.nokasegu.post_here.forum.service;

import io.github.nokasegu.post_here.forum.repository.ForumCommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ForumCommentService {

    private final ForumCommentRepository forumCommentRepository;
}
