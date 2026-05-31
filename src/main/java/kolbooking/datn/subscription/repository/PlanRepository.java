package kolbooking.datn.subscription.repository;

import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.subscription.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByCode(String code);

    List<Plan> findAllByActiveTrueOrderBySortOrderAscIdAsc();

    List<Plan> findAllByActiveTrueAndTargetRoleOrderBySortOrderAscIdAsc(Role targetRole);
}
