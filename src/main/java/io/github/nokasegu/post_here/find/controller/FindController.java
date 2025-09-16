package io.github.nokasegu.post_here.find.controller;

import io.github.nokasegu.post_here.find.service.FindService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class FindController {

    private final FindService findService;

    @GetMapping("/find-write")
    public String findController(Model model){
        return "/find/find-write";
    }
}
