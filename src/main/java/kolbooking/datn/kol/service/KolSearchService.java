package kolbooking.datn.kol.service;

import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.repository.BrandFavoriteRepository;
import kolbooking.datn.brand.repository.BrandProfileRepository;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.domain.KolProfileStatus;
import kolbooking.datn.kol.dto.KolSearchFilter;
import kolbooking.datn.kol.dto.KolSummaryResponse;
import kolbooking.datn.kol.repository.KolProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KolSearchService {

    private final KolProfileRepository kolProfileRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final BrandFavoriteRepository brandFavoriteRepository;

    @Transactional(readOnly = true)
    public PageResponse<KolSummaryResponse> search(KolSearchFilter filter, int page, int size) {
        if (size <= 0) size = 20;
        if (size > 100) size = 100;
        if (page < 0) page = 0;

        Sort sort = resolveSort(filter.sort());
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<KolProfile> result = kolProfileRepository.findAll(KolSpecification.matches(filter), pageable);
        Set<Long> favoriteIds = resolveFavoriteIds(result.getContent().stream().map(KolProfile::getId).toList());
        return PageResponse.of(result.map(k -> KolMapper.toSummary(k, favoriteIds.contains(k.getId()))));
    }

    @Transactional(readOnly = true)
    public List<KolSummaryResponse> featured(int limit) {
        if (limit <= 0 || limit > 50) limit = 10;
        Pageable p = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "avgRating", "reviewCount"));
        List<KolProfile> kols = kolProfileRepository.findAll(
                        (root, q, cb) -> cb.equal(root.get("status"), KolProfileStatus.APPROVED), p)
                .getContent();
        Set<Long> favoriteIds = resolveFavoriteIds(kols.stream().map(KolProfile::getId).toList());
        return kols.stream().map(k -> KolMapper.toSummary(k, favoriteIds.contains(k.getId()))).toList();
    }

    /**
     * Returns the ids the current BRAND has favorited among {@code kolIds}.
     * Anonymous callers and non-BRAND roles get an empty set — no extra query, isFavorite defaults to false.
     */
    private Set<Long> resolveFavoriteIds(List<Long> kolIds) {
        if (kolIds.isEmpty()) return Set.of();
        Long userId = SecurityUtils.currentUserIdSafe();
        if (userId == null || SecurityUtils.currentRole() != Role.BRAND) return Set.of();
        BrandProfile brand = brandProfileRepository.findByUserId(userId).orElse(null);
        if (brand == null) return Set.of();
        return new HashSet<>(brandFavoriteRepository.findFavoritedKolIds(brand.getId(), kolIds));
    }

    private Sort resolveSort(String key) {
        if (key == null) return Sort.by(Sort.Direction.DESC, "avgRating", "reviewCount");
        return switch (key) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "minPrice");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "minPrice");
            case "follower_desc" -> Sort.by(Sort.Direction.DESC, "maxFollowerCount");
            case "rating_desc" -> Sort.by(Sort.Direction.DESC, "avgRating");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "featured" -> Sort.by(Sort.Direction.DESC, "avgRating", "reviewCount");
            default -> Sort.by(Sort.Direction.DESC, "avgRating", "reviewCount");
        };
    }
}
