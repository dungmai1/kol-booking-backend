package kolbooking.datn.kol.repository;

import kolbooking.datn.kol.domain.KolPortfolioItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KolPortfolioItemRepository extends JpaRepository<KolPortfolioItem, Long> {
}
