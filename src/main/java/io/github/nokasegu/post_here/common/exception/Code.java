package io.github.nokasegu.post_here.common.exception;

import lombok.Getter;

@Getter
public enum Code {

    OK("000", "요청 처리에 성공했습니다"),


    LOGIN_FAIL("100", "계정 정보가 잘못되었습니다");

    private final String code;
    private final String value;

    Code(String code, String value) {
        this.code = code;
        this.value = value;
    }
}
