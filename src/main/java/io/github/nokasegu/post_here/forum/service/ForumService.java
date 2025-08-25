package io.github.nokasegu.post_here.forum.service;

import io.github.nokasegu.post_here.forum.repository.ForumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ForumService {

    private final ForumRepository forumRepository;
}
