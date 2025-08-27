package io.github.nokasegu.post_here.common.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDTO<T> {

    private int status;

    private String message;

    private T data;
}
