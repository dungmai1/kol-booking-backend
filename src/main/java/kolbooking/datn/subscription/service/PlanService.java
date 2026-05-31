package kolbooking.datn.subscription.service;

import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.subscription.domain.Plan;
import kolbooking.datn.subscription.dto.PlanResponse;
import kolbooking.datn.subscription.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private final PlanRepository planRepository;

    public List<PlanResponse> listActive(Role targetRole) {
        List<Plan> plans = targetRole == null
                ? planRepository.findAllByActiveTrueOrderBySortOrderAscIdAsc()
                : planRepository.findAllByActiveTrueAndTargetRoleOrderBySortOrderAscIdAsc(targetRole);
        return plans.stream().map(PlanMapper::toPlanResponse).toList();
    }

    public PlanResponse getByCode(String code) {
        Plan plan = planRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(
                        "Plan not found: " + code, ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND));
        return PlanMapper.toPlanResponse(plan);
    }

    Plan loadActivePlanByCode(String code) {
        Plan plan = planRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(
                        "Plan not found: " + code, ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND));
        if (!plan.isActive()) {
            throw new BusinessException(
                    "Plan is not active: " + code, ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        return plan;
    }
}
