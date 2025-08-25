package io.github.nokasegu.post_here.find.service;

import io.github.nokasegu.post_here.find.repository.FindRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindService {

    private final FindRepository findRepository;
}
