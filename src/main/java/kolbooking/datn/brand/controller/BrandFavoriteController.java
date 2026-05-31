package kolbooking.datn.brand.controller;

import kolbooking.datn.brand.service.BrandFavoriteService;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.kol.dto.KolSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/brands/me/favorites")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BRAND')")
public class BrandFavoriteController {

    private final BrandFavoriteService favoriteService;

    @PostMapping("/{kolId}")
    public ApiResponse<Void> add(@PathVariable("kolId") Long kolId) {
        favoriteService.add(kolId);
        return ApiResponse.ok("Added to favorites");
    }

    @DeleteMapping("/{kolId}")
    public ApiResponse<Void> remove(@PathVariable("kolId") Long kolId) {
        favoriteService.remove(kolId);
        return ApiResponse.ok("Removed from favorites");
    }

    @GetMapping
    public ApiResponse<PageResponse<KolSummaryResponse>> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return ApiResponse.ok(favoriteService.listMine(page, size));
    }
}
