package io.github.nokasegu.post_here.notification.service;

import com.google.gson.Gson;
import io.github.nokasegu.post_here.notification.domain.PushSubscriptionEntity;
import io.github.nokasegu.post_here.notification.repository.PushSubscriptionRepository;
import io.github.nokasegu.post_here.userInfo.domain.UserInfoEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.jose4j.lang.JoseException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebPushService {

    // ❌ 순환 의존 발생하므로 주입하지 마세요.
    // private final NotificationService notificationService;

    private final WebPushProperties props;
    private final PushSubscriptionRepository pushRepo;
    private final Gson gson = new Gson();

    private PushService buildClient() throws GeneralSecurityException {
        PushService svc = new PushService();
        svc.setSubject(props.getSubject());
        svc.setPublicKey(props.getPublicKey());
        svc.setPrivateKey(props.getPrivateKey());
        return svc;
    }

    public void sendToUser(UserInfoEntity target, Map<String, Object> payload) {
        List<PushSubscriptionEntity> subs = pushRepo.findAllByUser(target);
        if (subs.isEmpty()) return;

        final byte[] data = gson.toJson(payload).getBytes(StandardCharsets.UTF_8);

        try {
            PushService client = buildClient();

            for (PushSubscriptionEntity s : subs) {
                try {
                    Notification n = new Notification(s.getEndpoint(), s.getP256dh(), s.getAuth(), data);
                    client.send(n); // throws IOException, JoseException, ExecutionException, InterruptedException
                } catch (IOException | JoseException | ExecutionException e) {
                    log.warn("WebPush send fail endpoint={}", s.getEndpoint(), e);
                } catch (InterruptedException e) {
                    // 인터럽트 신호 복구
                    Thread.currentThread().interrupt();
                    log.warn("WebPush send interrupted endpoint={}", s.getEndpoint(), e);
                    return; // 현재 스레드가 인터럽트되었으니 루프 중단
                }
            }
        } catch (GeneralSecurityException e) {
            log.error("WebPush init error", e);
        }
    }
}
