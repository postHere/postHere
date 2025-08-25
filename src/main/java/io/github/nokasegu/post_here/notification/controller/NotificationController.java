package io.github.nokasegu.post_here.notification.controller;

import io.github.nokasegu.post_here.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
}
