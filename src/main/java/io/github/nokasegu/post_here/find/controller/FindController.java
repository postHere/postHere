package io.github.nokasegu.post_here.find.controller;

import io.github.nokasegu.post_here.find.service.FindService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class FindController {

    private final FindService findService;
}
