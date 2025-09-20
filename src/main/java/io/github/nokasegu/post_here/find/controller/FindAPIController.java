package io.github.nokasegu.post_here.find.controller;

import io.github.nokasegu.post_here.find.dto.FindPostSummaryDto;
import io.github.nokasegu.post_here.find.service.FindService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class FindAPIController {

    private final FindService findService;

    @GetMapping("/api/v1/finds/my-posts")
    public ResponseEntity<Page<FindPostSummaryDto>> getMyFinds(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 4) Pageable pageable) {

        if (userDetails == null) return ResponseEntity.status(401).build();

        Page<FindPostSummaryDto> result = findService.getMyFinds(userDetails.getUsername(), pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/v1/users/{nickname}/finds")
    public ResponseEntity<Page<FindPostSummaryDto>> getFindsForUser(
            @PathVariable String nickname,
            @PageableDefault(size = 4) Pageable pageable) {

        Page<FindPostSummaryDto> result = findService.getFindsByNickname(nickname, pageable);
        return ResponseEntity.ok(result);
    }
}