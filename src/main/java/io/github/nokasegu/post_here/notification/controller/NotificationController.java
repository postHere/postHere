// src/main/java/io/github/nokasegu/post_here/notification/controller/NotificationController.java
package io.github.nokasegu.post_here.notification.controller;

import io.github.nokasegu.post_here.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

/**
 * NotificationController (MVC 페이지 컨트롤러)
 * <p>
 * [역할]
 * - 알림센터 페이지(templates/notification/notification.html)로 라우팅하는 MVC 엔트리포인트.
 * - 실제 데이터 로딩/읽음 처리/빨간점(배지) 갱신은 프런트(/js/notification.js)와
 * REST 엔드포인트(예: POST /notification/list, /notification/read, /notification/unread-count)가 담당.
 * <p>
 * [분리 원칙]
 * - 이 클래스는 "페이지 이동(뷰 렌더)" 전담.
 * - 알림 목록/읽음/카운트와 같은 JSON API는 별도의 @RestController에서 제공.
 * (프런트는 fetch로 해당 API를 호출)
 * <p>
 * [예상 엔드포인트(참고, 구현 위치는 팀 컨벤션에 따름)]
 * - GET /notification  → return "notification/notification"
 * · 서버 사이드에서 사전 데이터 바인딩이 필요 없다면, 단순 뷰 반환만 수행.
 * · 보안: 인증 사용자만 접근 가능하도록 Spring Security 설정 권장.
 * <p>
 * [서비스 의존성]
 * - NotificationService는 향후 서버 사이드 사전 계산/집계가 필요할 때 사용 가능.
 * (현 시점에선 프런트 주도형이므로 미사용일 수 있음)
 */
@Controller
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // (참고) 팀 컨벤션상 이 컨트롤러에서 뷰 라우팅 메서드를 작성할 수 있습니다.
    // 예시:
    // @GetMapping("/notification")
    // public String notificationPage() {
    //     return "notification/notification";
    // }
}
