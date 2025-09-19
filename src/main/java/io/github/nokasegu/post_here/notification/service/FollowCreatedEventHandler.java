package io.github.nokasegu.post_here.notification.service;

import io.github.nokasegu.post_here.follow.domain.FollowingEntity;
import io.github.nokasegu.post_here.follow.repository.FollowingRepository;
import io.github.nokasegu.post_here.follow.service.FollowingService.FollowCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class FollowCreatedEventHandler {

    private final FollowingRepository followingRepository;
    private final NotificationService notificationService; // 프록시 경유 호출

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFollowCreated(FollowCreatedEvent ev) {
        log.info("[NOTI] onFollowCreated start followingId={}", ev.followingId());
        try {
            FollowingEntity entity = followingRepository.findById(ev.followingId()).orElse(null);
            if (entity == null) {
                log.warn("[NOTI] Follow entity not found. followingId={}", ev.followingId());
                return;
            }
            // 트랜잭션이 적용된 서비스 메서드를 프록시로 호출
            notificationService.createFollowAndPush(entity);

        } catch (Exception e) {
            log.warn("[NOTI] Follow notification failed. follower={}, followed={}, followingId={}",
                    ev.followerId(), ev.followedId(), ev.followingId(), e);
        }
    }
}
