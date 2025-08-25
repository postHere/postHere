package io.github.nokasegu.post_here.forum.service;

import io.github.nokasegu.post_here.forum.repository.ForumLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ForumLikeService {

    private final ForumLikeRepository forumLikeRepository;
}
