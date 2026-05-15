package kolbooking.datn.kol.repository;

import kolbooking.datn.kol.domain.KolSocialChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface KolSocialChannelRepository extends JpaRepository<KolSocialChannel, Long> {

    /**
     * Computes max follower_count for a KOL profile. Used to maintain the denormalized
     * {@code kol_profile.max_follower_count} column after channel CRUD.
     */
    @Query("SELECT MAX(c.followerCount) FROM KolSocialChannel c WHERE c.kolProfile.id = :profileId")
    Optional<Long> findMaxFollowerByProfileId(@Param("profileId") Long profileId);
}
