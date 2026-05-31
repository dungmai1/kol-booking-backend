package kolbooking.datn.subscription.repository;

import kolbooking.datn.subscription.domain.Subscription;
import kolbooking.datn.subscription.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findFirstByUserIdAndStatusOrderByExpiresAtDesc(Long userId, SubscriptionStatus status);

    Optional<Subscription> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Subscription> findByExternalRef(String externalRef);

    List<Subscription> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
