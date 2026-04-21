package kolbooking.datn.brand.service;

import jakarta.transaction.Transactional;
import kolbooking.datn.brand.domain.BrandFavorite;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.repository.BrandFavoriteRepository;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.dto.KolSummaryResponse;
import kolbooking.datn.kol.repository.KolProfileRepository;
import kolbooking.datn.kol.service.KolMapper;
import kolbooking.datn.kol.service.KolProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandFavoriteService {

    private final BrandFavoriteRepository favoriteRepository;
    private final KolProfileRepository kolProfileRepository;
    private final KolProfileService kolProfileService;
    private final BrandProfileService brandProfileService;

    @Transactional
    public void add(Long kolProfileId) {
        BrandProfile brand = brandProfileService.getCurrentBrandProfile();
        KolProfile kol = kolProfileService.requireApprovedById(kolProfileId);
        BrandFavorite.BrandFavoriteId id = new BrandFavorite.BrandFavoriteId(brand.getId(), kol.getId());
        if (favoriteRepository.existsById(id)) return;
        favoriteRepository.save(BrandFavorite.builder().id(id).build());
    }

    @Transactional
    public void remove(Long kolProfileId) {
        BrandProfile brand = brandProfileService.getCurrentBrandProfile();
        BrandFavorite.BrandFavoriteId id = new BrandFavorite.BrandFavoriteId(brand.getId(), kolProfileId);
        favoriteRepository.deleteById(id);
    }

    @Transactional
    public PageResponse<KolSummaryResponse> listMine(int page, int size) {
        BrandProfile brand = brandProfileService.getCurrentBrandProfile();
        if (size <= 0 || size > 100) size = 20;
        if (page < 0) page = 0;
        Page<BrandFavorite> favs = favoriteRepository.findByIdBrandProfileId(
                brand.getId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<Long> kolIds = favs.getContent().stream().map(f -> f.getId().getKolProfileId()).toList();
        List<KolProfile> kols = kolProfileRepository.findAllById(kolIds);
        List<KolSummaryResponse> content = kols.stream().map(KolMapper::toSummary).toList();
        return new PageResponse<>(content, favs.getNumber(), favs.getSize(),
                favs.getTotalElements(), favs.getTotalPages());
    }
}
