package io.github.nokasegu.post_here.userInfo.controller;

import io.github.nokasegu.post_here.userInfo.service.UserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class UserInfoController {

    private final UserInfoService userInfoService;
}
