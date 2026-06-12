package kolbooking.datn.kol.service;

import org.springframework.data.domain.Sort;

/**
 * Maps {@code sort} query param values to Spring Data {@link Sort}.
 * Accepts both frontend aliases ({@code followers}, {@code rating}) and legacy names
 * ({@code follower_desc}, {@code rating_desc}).
 */
public final class KolSortResolver {

    private KolSortResolver() {}

    public static Sort resolve(String key) {
        if (key == null) {
            return Sort.by(Sort.Direction.DESC, "avgRating", "reviewCount");
        }
        String normalized = switch (key) {
            case "followers" -> "follower_desc";
            case "rating" -> "rating_desc";
            default -> key;
        };
        return switch (normalized) {
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
