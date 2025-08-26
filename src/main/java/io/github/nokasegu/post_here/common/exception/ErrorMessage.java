package io.github.nokasegu.post_here.common.exception;

import lombok.Getter;

@Getter
public enum ErrorMessage {

    LOGIN_FAIL("100", "계정 정보가 잘못되었습니다");

    private final String code;
    private final String value;

    ErrorMessage(String code, String value) {
        this.code = code;
        this.value = value;
    }
}
