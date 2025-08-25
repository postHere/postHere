package io.github.nokasegu.post_here.park.service;

import io.github.nokasegu.post_here.park.repository.ParkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParkService {

    private final ParkRepository parkRepository;
}
