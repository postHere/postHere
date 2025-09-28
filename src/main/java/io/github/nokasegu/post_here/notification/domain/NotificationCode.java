package io.github.nokasegu.post_here.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum NotificationCode {
    FOLLOW("FOLLOW"),
    COMMENT("COMMENT"),
    FIND_FOUND("FIND_FOUND"),
    PARK_UPDATE("PARK_UPDATE");


    private final String code;
}