package io.github.nokasegu.post_here.userInfo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckLoginDto {
    String username;
    boolean isAuthenticated;
}
