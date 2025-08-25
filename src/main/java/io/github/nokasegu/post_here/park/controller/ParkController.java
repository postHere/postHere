package io.github.nokasegu.post_here.park.controller;

import io.github.nokasegu.post_here.park.service.ParkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ParkController {

    private final ParkService parkService;
}
