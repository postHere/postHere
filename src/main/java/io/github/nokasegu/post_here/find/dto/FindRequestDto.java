package io.github.nokasegu.post_here.find.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@Data
public class FindRequestDto {

    private MultipartFile content_capture;
    private String expiration_date;
    private Double lat;
    private Double lng;
}
