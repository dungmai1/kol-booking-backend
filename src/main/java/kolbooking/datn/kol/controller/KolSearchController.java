package kolbooking.datn.kol.controller;

import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.kol.domain.Gender;
import kolbooking.datn.kol.domain.Platform;
import kolbooking.datn.kol.dto.KolSearchFilter;
import kolbooking.datn.kol.dto.KolSummaryResponse;
import kolbooking.datn.kol.service.KolSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/kols")
@RequiredArgsConstructor
public class KolSearchController {

    private final KolSearchService kolSearchService;

    @GetMapping("/search")
    public ApiResponse<PageResponse<KolSummaryResponse>> search(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "categoryIds", required = false) Set<Long> categoryIds,
            @RequestParam(name = "platforms", required = false) Set<Platform> platforms,
            @RequestParam(name = "minFollower", required = false) Long minFollower,
            @RequestParam(name = "maxFollower", required = false) Long maxFollower,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "city", required = false) String city,
            @RequestParam(name = "country", required = false) String country,
            @RequestParam(name = "gender", required = false) Gender gender,
            @RequestParam(name = "minRating", required = false) BigDecimal minRating,
            @RequestParam(name = "sort", required = false, defaultValue = "featured") String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        KolSearchFilter filter = new KolSearchFilter(
                q, categoryIds, platforms, minFollower, maxFollower,
                minPrice, maxPrice, city, country, gender, minRating, sort);
        return ApiResponse.ok(kolSearchService.search(filter, page, size));
    }

    @GetMapping("/featured")
    public ApiResponse<List<KolSummaryResponse>> featured(
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        return ApiResponse.ok(kolSearchService.featured(limit));
    }
}
