package kolbooking.datn.product.service;

import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.domain.BrandProfileStatus;
import kolbooking.datn.brand.repository.BrandProfileRepository;
import kolbooking.datn.brand.service.BrandProfileService;
import kolbooking.datn.common.domain.Category;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import kolbooking.datn.common.repository.CategoryRepository;
import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.kol.repository.KolProfileRepository;
import kolbooking.datn.product.domain.ApplicationStatus;
import kolbooking.datn.product.domain.Product;
import kolbooking.datn.product.domain.ProductStatus;
import kolbooking.datn.product.dto.ProductCreateRequest;
import kolbooking.datn.product.dto.ProductResponse;
import kolbooking.datn.product.dto.ProductSearchFilter;
import kolbooking.datn.product.dto.ProductUpdateRequest;
import kolbooking.datn.product.repository.ProductApplicationRepository;
import kolbooking.datn.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductApplicationRepository applicationRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final BrandProfileService brandProfileService;
    private final CategoryRepository categoryRepository;
    private final KolProfileRepository kolProfileRepository;

    // ---- Brand-owned operations ----------------------------------------------------------------

    @Transactional
    public ProductResponse create(ProductCreateRequest req) {
        BrandProfile brand = requireApprovedBrand();
        validateCategory(req.categoryId());

        Product p = Product.builder()
                .brandProfileId(brand.getId())
                .title(req.title())
                .description(req.description())
                .imageUrl(req.imageUrl())
                .attachmentUrl(blankToNull(req.attachmentUrl()))
                .budget(req.budget())
                .categoryId(req.categoryId())
                .requiredPlatform(req.requiredPlatform())
                .minFollowers(req.minFollowers())
                .slots(req.slots() == null ? 1 : req.slots())
                .status(ProductStatus.OPEN)
                .deadline(req.deadline())
                .applicationCount(0)
                .build();
        p = productRepository.save(p);
        log.info("Product created: id={}, brandProfileId={}", p.getId(), brand.getId());
        return enrich(p, false);
    }

    @Transactional
    public ProductResponse update(Long id, ProductUpdateRequest req) {
        Product p = requireOwnedProduct(id);
        if (p.getStatus() != ProductStatus.OPEN) {
            throw new BusinessException("Chỉ có thể sửa sản phẩm đang mở (OPEN)",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        if (req.title() != null) p.setTitle(req.title());
        if (req.description() != null) p.setDescription(req.description());
        if (req.imageUrl() != null) p.setImageUrl(req.imageUrl());
        if (req.attachmentUrl() != null) p.setAttachmentUrl(blankToNull(req.attachmentUrl()));
        if (req.budget() != null) p.setBudget(req.budget());
        if (req.categoryId() != null) {
            validateCategory(req.categoryId());
            p.setCategoryId(req.categoryId());
        }
        if (req.requiredPlatform() != null) p.setRequiredPlatform(req.requiredPlatform());
        if (req.minFollowers() != null) p.setMinFollowers(req.minFollowers());
        if (req.slots() != null) p.setSlots(req.slots());
        if (req.deadline() != null) p.setDeadline(req.deadline());
        return enrich(productRepository.save(p), false);
    }

    @Transactional
    public ProductResponse close(Long id) {
        Product p = requireOwnedProduct(id);
        p.setStatus(ProductStatus.CLOSED);
        return enrich(productRepository.save(p), false);
    }

    @Transactional
    public ProductResponse reopen(Long id) {
        Product p = requireOwnedProduct(id);
        p.setStatus(ProductStatus.OPEN);
        return enrich(productRepository.save(p), false);
    }

    @Transactional
    public void delete(Long id) {
        Product p = requireOwnedProduct(id);
        if (p.getApplicationCount() != null && p.getApplicationCount() > 0) {
            throw new BusinessException("Không thể xoá sản phẩm đã có ứng tuyển — hãy đóng (close) thay vì xoá",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        productRepository.delete(p);
        log.info("Product deleted: id={}", id);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listMine(int page, int size) {
        BrandProfile brand = brandProfileService.getCurrentBrandProfile();
        Page<Product> result = productRepository.findByBrandProfileId(
                brand.getId(), pageable(page, size));
        return mapPage(result, null);
    }

    // ---- Public browse -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> browse(ProductSearchFilter filter, int page, int size) {
        Page<Product> result = productRepository.findAll(
                ProductSpecification.matches(filter), pageable(page, size));
        return mapPage(result, currentKolProfileId());
    }

    @Transactional(readOnly = true)
    public ProductResponse getPublic(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", id));
        if (p.getStatus() != ProductStatus.OPEN && !isOwner(p)) {
            // Accepted KOLs retain read access even after the product closes.
            Long kolId = currentKolProfileId();
            if (kolId == null || !applicationRepository.existsByProductIdAndKolProfileIdAndStatus(
                    p.getId(), kolId, ApplicationStatus.ACCEPTED)) {
                throw ResourceNotFoundException.of("Product", id);
            }
            return enrich(p, true);
        }
        Long kolId = currentKolProfileId();
        boolean hasApplied = kolId != null
                && applicationRepository.existsByProductIdAndKolProfileIdAndStatusIn(
                        p.getId(), kolId, ApplicationStatus.ACTIVE);
        return enrich(p, hasApplied);
    }

    // ---- Internal helpers ----------------------------------------------------------------------

    /** Returns the product if it exists and is owned by the current brand; else 404/403. */
    public Product requireOwnedProduct(Long id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", id));
        BrandProfile brand = brandProfileService.getByUserId(SecurityUtils.currentUserId());
        if (!p.getBrandProfileId().equals(brand.getId())) {
            throw new BusinessException("Không phải sản phẩm của bạn", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        return p;
    }

    private BrandProfile requireApprovedBrand() {
        BrandProfile brand = brandProfileService.getCurrentBrandProfile();
        if (brand.getStatus() != BrandProfileStatus.APPROVED) {
            throw new BusinessException("Hồ sơ Brand cần được duyệt trước khi đăng sản phẩm",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        return brand;
    }

    private void validateCategory(Long categoryId) {
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new BusinessException("categoryId không tồn tại",
                    ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
    }

    private boolean isOwner(Product p) {
        Long userId = SecurityUtils.currentUserIdSafe();
        if (userId == null || SecurityUtils.currentRole() != Role.BRAND) {
            return false;
        }
        return brandProfileRepository.findByUserId(userId)
                .map(b -> b.getId().equals(p.getBrandProfileId()))
                .orElse(false);
    }

    private Long currentKolProfileId() {
        Long userId = SecurityUtils.currentUserIdSafe();
        if (userId == null || SecurityUtils.currentRole() != Role.KOL) {
            return null;
        }
        return kolProfileRepository.findByUserId(userId).map(k -> k.getId()).orElse(null);
    }

    private ProductResponse enrich(Product p, boolean hasApplied) {
        String brandName = brandProfileRepository.findById(p.getBrandProfileId())
                .map(BrandProfile::getCompanyName).orElse(null);
        String categoryName = p.getCategoryId() == null ? null
                : categoryRepository.findById(p.getCategoryId()).map(Category::getName).orElse(null);
        return ProductMapper.toDto(p, brandName, categoryName, hasApplied);
    }

    private PageResponse<ProductResponse> mapPage(Page<Product> page, Long viewerKolId) {
        List<Product> content = page.getContent();

        Set<Long> brandIds = content.stream().map(Product::getBrandProfileId).collect(Collectors.toSet());
        Map<Long, String> brandNames = brandIds.isEmpty() ? Map.of()
                : brandProfileRepository.findAllById(brandIds).stream()
                    .collect(Collectors.toMap(BrandProfile::getId, BrandProfile::getCompanyName));

        Set<Long> catIds = content.stream().map(Product::getCategoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> catNames = catIds.isEmpty() ? Map.of()
                : categoryRepository.findAllById(catIds).stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName));

        Set<Long> applied = (viewerKolId == null || content.isEmpty()) ? Set.of()
                : new HashSet<>(applicationRepository.findAppliedProductIds(
                    viewerKolId, content.stream().map(Product::getId).toList(),
                    ApplicationStatus.ACTIVE));

        return PageResponse.of(page.map(p -> ProductMapper.toDto(
                p,
                brandNames.get(p.getBrandProfileId()),
                p.getCategoryId() == null ? null : catNames.get(p.getCategoryId()),
                applied.contains(p.getId()))));
    }

    private static PageRequest pageable(int page, int size) {
        if (size <= 0 || size > 100) size = 20;
        if (page < 0) page = 0;
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
