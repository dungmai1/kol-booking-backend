package kolbooking.datn.kol.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class KolSortResolverTest {

    @Test
    void resolve_followersAlias_sortsByMaxFollowerCountDesc() {
        Sort sort = KolSortResolver.resolve("followers");
        assertThat(sort.getOrderFor("maxFollowerCount").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void resolve_ratingAlias_sortsByAvgRatingDesc() {
        Sort sort = KolSortResolver.resolve("rating");
        assertThat(sort.getOrderFor("avgRating").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void resolve_legacyFollowerDesc_stillWorks() {
        Sort sort = KolSortResolver.resolve("follower_desc");
        assertThat(sort.getOrderFor("maxFollowerCount").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void resolve_priceAsc_sortsByMinPriceAsc() {
        Sort sort = KolSortResolver.resolve("price_asc");
        assertThat(sort.getOrderFor("minPrice").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void resolve_featured_sortsByRatingAndReviewCount() {
        Sort sort = KolSortResolver.resolve("featured");
        assertThat(sort.getOrderFor("avgRating").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sort.getOrderFor("reviewCount").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void resolve_null_defaultsToFeatured() {
        Sort sort = KolSortResolver.resolve(null);
        assertThat(sort.getOrderFor("avgRating").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sort.getOrderFor("reviewCount").getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
