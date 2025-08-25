package io.github.nokasegu.post_here.find.controller;

import io.github.nokasegu.post_here.find.service.FindService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
@RequestMapping("/test")
public class FindController {

    private final FindService findService;

    @GetMapping("/find")
    @ResponseBody
    public String testController(){
        return "hello";
    }
}
