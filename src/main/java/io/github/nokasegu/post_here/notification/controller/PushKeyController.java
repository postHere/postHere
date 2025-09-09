// src/main/java/io/github/nokasegu/post_here/notification/controller/PushKeyController.java
package io.github.nokasegu.post_here.notification.controller;

import io.github.nokasegu.post_here.notification.service.WebPushProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PushKeyController {
    private final WebPushProperties props;

    @GetMapping(value = "/push/vapid-public-key", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> key() {
        return Map.of("publicKey", props.getPublicKey());
    }
}
