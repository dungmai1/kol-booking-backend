package kolbooking.datn.auth.repository;

import kolbooking.datn.auth.domain.TokenPurpose;
import kolbooking.datn.auth.domain.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);

    @Modifying
    @Query("""
           update VerificationToken v
              set v.usedAt = CURRENT_TIMESTAMP
            where v.userId = :userId
              and v.purpose = :purpose
              and v.usedAt is null
           """)
    int invalidateActiveForUser(@Param("userId") Long userId,
                                @Param("purpose") TokenPurpose purpose);
}
