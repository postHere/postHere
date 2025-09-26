package io.github.nokasegu.post_here.location.service;

import io.github.nokasegu.post_here.forum.domain.ForumAreaEntity;
import io.github.nokasegu.post_here.forum.service.ForumService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final ForumService forumService;

    public ForumAreaEntity getForumArea(String address) {
        return forumService.getAreaByAddress(address);
    }
}
