package io.github.nokasegu.post_here.notification.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class NotificationPageController {
    @GetMapping("/notification")
    public String page() {
        return "notification/notification";
    }
}
