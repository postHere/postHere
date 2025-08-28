package io.github.nokasegu.post_here.common.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WrapperDTO<T> {

    private String status;

    private String message;

    private T data;
}
