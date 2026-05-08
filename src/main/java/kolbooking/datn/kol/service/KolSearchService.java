package kolbooking.datn.kol.service;

import kolbooking.datn.common.dto.PageResponse;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class KolSearchService {

    private final KolProfileRepository kolProfileRepository;

    @Transactional(readOnly = true)
    public PageResponse<KolSummaryResponse> search(KolSearchFilter filter, int page, int size) {
        if (size <= 0) size = 20;
        if (size > 100) size = 100;
        if (page < 0) page = 0;

        Sort sort = resolveSort(filter.sort());
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<KolProfile> result = kolProfileRepository.findAll(KolSpecification.matches(filter), pageable);
        List<KolSummaryResponse> content = result.getContent().stream().map(KolMapper::toSummary).toList();
        return new PageResponse<>(
                content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public List<KolSummaryResponse> featured(int limit) {
        if (limit <= 0 || limit > 50) limit = 10;
        Pageable p = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "avgRating", "reviewCount"));
        return kolProfileRepository.findAll(
                        (root, q, cb) -> cb.equal(root.get("status"), KolProfileStatus.APPROVED), p)
                .getContent().stream().map(KolMapper::toSummary).toList();
    }

    private Sort resolveSort(String key) {
        if (key == null) return Sort.by(Sort.Direction.DESC, "avgRating", "reviewCount");
        return switch (key) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "avgRating");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "avgRating");
            case "follower_desc" -> Sort.by(Sort.Direction.DESC, "reviewCount");
            case "rating_desc" -> Sort.by(Sort.Direction.DESC, "avgRating");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "featured" -> Sort.by(Sort.Direction.DESC, "avgRating", "reviewCount");
            default -> Sort.by(Sort.Direction.DESC, "avgRating", "reviewCount");
        };
    }
}
