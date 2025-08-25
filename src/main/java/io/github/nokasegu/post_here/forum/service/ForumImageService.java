package io.github.nokasegu.post_here.forum.service;

import io.github.nokasegu.post_here.forum.repository.ForumImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ForumImageService {

    private final ForumImageRepository forumImageRepository;
}
