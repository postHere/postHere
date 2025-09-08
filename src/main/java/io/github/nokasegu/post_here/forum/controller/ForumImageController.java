package io.github.nokasegu.post_here.forum.controller;

import io.github.nokasegu.post_here.common.dto.WrapperDTO;
import io.github.nokasegu.post_here.common.exception.Code;
import io.github.nokasegu.post_here.forum.service.ForumImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ForumImageController {

    private final ForumImageService forumImageService;

    @ResponseBody
    @PostMapping(value = "/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public WrapperDTO<List<String>> uploadImages(@RequestPart("images") List<MultipartFile> images) throws IOException {

        List<String> imageUrls = images.stream()
                .map(image -> {
                    try {
                        return forumImageService.uploadImage(image, "forum-images");
                    } catch (IOException e) {
                        throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.", e);
                    }
                })
                .collect(Collectors.toList());

        return WrapperDTO.<List<String>>builder()
                .status(Code.OK.getCode())
                .message(Code.OK.getValue())
                .data(imageUrls)
                .build();
    }
}
