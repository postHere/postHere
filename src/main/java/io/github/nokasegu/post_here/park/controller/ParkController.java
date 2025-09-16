package io.github.nokasegu.post_here.park.controller;

import io.github.nokasegu.post_here.park.service.ParkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ParkController {

    private final ParkService parkService;

    @GetMapping("/park-write")
    public String parkWrite() {
        return "/park/park-write";
    }
}
