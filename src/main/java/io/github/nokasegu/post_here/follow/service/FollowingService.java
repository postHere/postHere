package io.github.nokasegu.post_here.follow.service;

import io.github.nokasegu.post_here.follow.repository.FollowingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FollowingService {

    private final FollowingRepository followingRepository;
}
