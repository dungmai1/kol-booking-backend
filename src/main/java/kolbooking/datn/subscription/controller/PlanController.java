package kolbooking.datn.subscription.controller;

import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.subscription.dto.PlanResponse;
import kolbooking.datn.subscription.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public ApiResponse<List<PlanResponse>> list(@RequestParam(required = false) Role targetRole) {
        return ApiResponse.ok(planService.listActive(targetRole));
    }

    @GetMapping("/{code}")
    public ApiResponse<PlanResponse> get(@PathVariable String code) {
        return ApiResponse.ok(planService.getByCode(code));
    }
}
