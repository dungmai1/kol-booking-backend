package kolbooking.datn.kol.service;

import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.booking.domain.BookingStatus;
import kolbooking.datn.booking.repository.BookingRepository;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.repository.BrandFavoriteRepository;
import kolbooking.datn.brand.repository.BrandProfileRepository;
import kolbooking.datn.common.domain.Category;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.kol.domain.Gender;
import kolbooking.datn.kol.domain.KolPricingPackage;
import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.domain.KolProfileStatus;
import kolbooking.datn.kol.domain.KolSocialChannel;
import kolbooking.datn.kol.domain.Platform;
import kolbooking.datn.kol.domain.PricingPackageType;
import kolbooking.datn.kol.dto.KolCandidatePlatformResponse;
import kolbooking.datn.kol.dto.KolCandidateResponse;
import kolbooking.datn.kol.dto.KolCandidateSearchRequest;
import kolbooking.datn.kol.dto.KolCandidateSearchResponse;
import kolbooking.datn.kol.dto.KolSearchFilter;
import kolbooking.datn.kol.dto.KolSummaryResponse;
import kolbooking.datn.kol.repository.KolProfileRepository;
import org.springframework.data.jpa.domain.Specification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KolSearchService {

    private final KolProfileRepository kolProfileRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final BrandFavoriteRepository brandFavoriteRepository;
    private final BookingRepository bookingRepository;

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

    @Transactional(readOnly = true)
    public KolCandidateSearchResponse searchCandidates(KolCandidateSearchRequest request) {
        int limit = request.limit() == null ? 50 : request.limit();
        if (limit <= 0) limit = 50;
        if (limit > 100) limit = 100;

        Page<KolProfile> result = kolProfileRepository.findAll(
                candidateSpecification(request),
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "avgRating", "reviewCount"))
        );
        List<KolProfile> kols = result.getContent();
        List<Long> kolIds = kols.stream().map(KolProfile::getId).toList();

        Map<Long, Long> completedCounts = countBookings(kolIds, EnumSet.of(BookingStatus.COMPLETED));
        Map<Long, Long> acceptedCounts = countBookings(kolIds, EnumSet.of(
                BookingStatus.ACCEPTED,
                BookingStatus.IN_PROGRESS,
                BookingStatus.DELIVERED,
                BookingStatus.COMPLETED,
                BookingStatus.DISPUTED
        ));
        Map<Long, Long> rejectedCounts = countBookings(kolIds, EnumSet.of(BookingStatus.REJECTED));

        List<KolCandidateResponse> items = kols.stream()
                .map(kol -> toCandidate(
                        kol,
                        completedCounts.getOrDefault(kol.getId(), 0L),
                        acceptanceRate(
                                acceptedCounts.getOrDefault(kol.getId(), 0L),
                                rejectedCounts.getOrDefault(kol.getId(), 0L)
                        )
                ))
                .toList();
        return new KolCandidateSearchResponse(items);
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

    private Specification<KolProfile> candidateSpecification(KolCandidateSearchRequest request) {
        String category = normalizeText(request.category());
        Set<Platform> platforms = parsePlatforms(request.platforms());
        Gender gender = parseGender(request.gender());
        PricingPackageType serviceType = parseServiceType(request.serviceType());
        String location = normalizeText(request.location());

        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), KolProfileStatus.APPROVED));

            if (category != null && query != null) {
                jakarta.persistence.criteria.Subquery<Long> sub = query.subquery(Long.class);
                jakarta.persistence.criteria.Root<KolProfile> corr = sub.correlate(root);
                jakarta.persistence.criteria.Join<KolProfile, Category> catJoin = corr.join("categories");
                sub.select(cb.literal(1L))
                        .where(cb.equal(cb.lower(catJoin.get("slug")), category));
                predicates.add(cb.exists(sub));
            }

            if (location != null) {
                String like = "%" + location + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("city")), like),
                        cb.like(cb.lower(root.get("country")), like)
                ));
            }

            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }

            if ((!platforms.isEmpty() || request.minFollowers() != null || request.maxFollowers() != null)
                    && query != null) {
                jakarta.persistence.criteria.Subquery<Long> sub = query.subquery(Long.class);
                jakarta.persistence.criteria.Root<KolProfile> corr = sub.correlate(root);
                jakarta.persistence.criteria.Join<KolProfile, KolSocialChannel> chJoin = corr.join("channels");
                List<jakarta.persistence.criteria.Predicate> channelPredicates = new ArrayList<>();
                if (!platforms.isEmpty()) {
                    channelPredicates.add(chJoin.get("platform").in(platforms));
                }
                if (request.minFollowers() != null) {
                    channelPredicates.add(cb.greaterThanOrEqualTo(chJoin.get("followerCount"), request.minFollowers()));
                }
                if (request.maxFollowers() != null) {
                    channelPredicates.add(cb.lessThanOrEqualTo(chJoin.get("followerCount"), request.maxFollowers()));
                }
                sub.select(cb.literal(1L)).where(channelPredicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
                predicates.add(cb.exists(sub));
            }

            if ((request.minBudget() != null || request.maxBudget() != null || serviceType != null) && query != null) {
                jakarta.persistence.criteria.Subquery<Long> sub = query.subquery(Long.class);
                jakarta.persistence.criteria.Root<KolProfile> corr = sub.correlate(root);
                jakarta.persistence.criteria.Join<KolProfile, KolPricingPackage> pkgJoin = corr.join("pricingPackages");
                List<jakarta.persistence.criteria.Predicate> packagePredicates = new ArrayList<>();
                if (request.minBudget() != null) {
                    packagePredicates.add(cb.greaterThanOrEqualTo(pkgJoin.get("price"), request.minBudget()));
                }
                if (request.maxBudget() != null) {
                    packagePredicates.add(cb.lessThanOrEqualTo(pkgJoin.get("price"), request.maxBudget()));
                }
                if (serviceType != null) {
                    packagePredicates.add(cb.equal(pkgJoin.get("type"), serviceType));
                }
                sub.select(cb.literal(1L)).where(packagePredicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
                predicates.add(cb.exists(sub));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private KolCandidateResponse toCandidate(KolProfile kol, long completedBookingCount, Double acceptanceRate) {
        List<String> categories = kol.getCategories().stream()
                .map(Category::getSlug)
                .filter(slug -> slug != null && !slug.isBlank())
                .sorted()
                .toList();

        List<KolCandidatePlatformResponse> platforms = kol.getChannels().stream()
                .sorted(Comparator.comparing(c -> c.getPlatform().name()))
                .map(channel -> new KolCandidatePlatformResponse(
                        channel.getPlatform().name().toLowerCase(Locale.ROOT),
                        channel.getUrl(),
                        channel.getFollowerCount() == null ? 0L : channel.getFollowerCount(),
                        engagementRatio(channel.getEngagementRate()),
                        null
                ))
                .toList();

        return new KolCandidateResponse(
                kol.getId(),
                kol.getDisplayName(),
                kol.getAvatarUrl(),
                kol.getBio(),
                formatLocation(kol.getCity(), kol.getCountry()),
                kol.getGender() == null ? null : kol.getGender().name().toLowerCase(Locale.ROOT),
                categories,
                platforms,
                toLong(kol.getMinPrice()),
                toDouble(kol.getAvgRating()),
                completedBookingCount,
                acceptanceRate
        );
    }

    private Map<Long, Long> countBookings(List<Long> kolIds, Set<BookingStatus> statuses) {
        if (kolIds.isEmpty() || statuses.isEmpty()) return Map.of();
        return bookingRepository.countByKolProfileIdsAndStatuses(kolIds, statuses).stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue(),
                        Long::sum
                ));
    }

    private Double acceptanceRate(long acceptedCount, long rejectedCount) {
        long totalDecisions = acceptedCount + rejectedCount;
        if (totalDecisions == 0) return null;
        return BigDecimal.valueOf(acceptedCount)
                .divide(BigDecimal.valueOf(totalDecisions), 4, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private Double engagementRatio(BigDecimal value) {
        if (value == null) return null;
        BigDecimal ratio = value.compareTo(BigDecimal.ONE) > 0
                ? value.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                : value;
        return ratio.doubleValue();
    }

    private Long toLong(BigDecimal value) {
        return value == null ? null : value.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

    private String formatLocation(String city, String country) {
        boolean hasCity = city != null && !city.isBlank();
        boolean hasCountry = country != null && !country.isBlank();
        if (hasCity && hasCountry) return city + ", " + country;
        if (hasCity) return city;
        if (hasCountry) return country;
        return null;
    }

    private Set<Platform> parsePlatforms(List<String> values) {
        if (values == null || values.isEmpty()) return Set.of();
        return values.stream()
                .map(value -> parseEnum(Platform.class, value, "platforms"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Gender parseGender(String value) {
        return parseEnum(Gender.class, value, "gender");
    }

    private PricingPackageType parseServiceType(String value) {
        return parseEnum(PricingPackageType.class, value, "serviceType");
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String field) {
        String normalized = normalizeText(value);
        if (normalized == null) return null;
        try {
            return Enum.valueOf(enumType, normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(
                    field + ": invalid value '" + value + "'",
                    ErrorCode.VALIDATION_FAILED,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
