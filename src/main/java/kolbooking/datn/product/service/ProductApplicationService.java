package kolbooking.datn.product.service;

import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.repository.AppUserRepository;
import kolbooking.datn.auth.service.EmailService;
import kolbooking.datn.booking.domain.Booking;
import kolbooking.datn.booking.service.BookingService;
import kolbooking.datn.brand.service.BrandProfileService;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.domain.KolProfileStatus;
import kolbooking.datn.kol.repository.KolProfileRepository;
import kolbooking.datn.kol.service.KolProfileService;
import kolbooking.datn.notification.domain.NotificationType;
import kolbooking.datn.notification.service.NotificationService;
import kolbooking.datn.product.domain.ApplicationStatus;
import kolbooking.datn.product.domain.Product;
import kolbooking.datn.product.domain.ProductApplication;
import kolbooking.datn.product.domain.ProductStatus;
import kolbooking.datn.product.dto.ProductApplicationCreateRequest;
import kolbooking.datn.product.dto.ProductApplicationResponse;
import kolbooking.datn.product.repository.ProductApplicationRepository;
import kolbooking.datn.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductApplicationService {

    private final ProductRepository productRepository;
    private final ProductApplicationRepository applicationRepository;
    private final ProductService productService;
    private final KolProfileService kolProfileService;
    private final KolProfileRepository kolProfileRepository;
    private final BrandProfileService brandProfileService;
    private final BookingService bookingService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AppUserRepository userRepository;

    // ---- KOL side ------------------------------------------------------------------------------

    @Transactional
    public ProductApplicationResponse apply(Long productId, ProductApplicationCreateRequest req) {
        KolProfile kol = currentKol();
        if (kol.getStatus() != KolProfileStatus.APPROVED) {
            throw new BusinessException("Hồ sơ KOL cần được duyệt trước khi ứng tuyển",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));
        if (product.getStatus() != ProductStatus.OPEN) {
            throw new BusinessException("Sản phẩm không còn nhận ứng tuyển",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        if (applicationRepository.existsByProductIdAndKolProfileId(productId, kol.getId())) {
            throw new BusinessException("Bạn đã ứng tuyển sản phẩm này",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }

        ProductApplication application = ProductApplication.builder()
                .productId(productId)
                .kolProfileId(kol.getId())
                .message(req == null ? null : req.message())
                .proposedPrice(req == null ? null : req.proposedPrice())
                .status(ApplicationStatus.PENDING)
                .build();
        try {
            application = applicationRepository.saveAndFlush(application);
        } catch (DataIntegrityViolationException ex) {
            // Lost the race against the unique (product_id, kol_profile_id) constraint.
            throw new BusinessException("Bạn đã ứng tuyển sản phẩm này",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }

        productRepository.incrementApplicationCount(product.getId());

        Long brandUserId = brandProfileService.getById(product.getBrandProfileId()).getUserId();
        notifyUser(brandUserId, NotificationType.PRODUCT_APPLICATION_RECEIVED,
                "Ứng tuyển mới",
                "KOL " + kol.getDisplayName() + " đã ứng tuyển sản phẩm: " + product.getTitle(),
                "/products/" + product.getId() + "/applications");

        log.info("KOL {} applied to product {} (applicationId={})", kol.getId(), productId, application.getId());
        return ProductMapper.toDto(application, kol);
    }

    @Transactional
    public ProductApplicationResponse withdraw(Long applicationId) {
        KolProfile kol = currentKol();
        ProductApplication a = getApplication(applicationId);
        if (!a.getKolProfileId().equals(kol.getId())) {
            throw new BusinessException("Không phải ứng tuyển của bạn", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        if (a.getStatus() != ApplicationStatus.PENDING && a.getStatus() != ApplicationStatus.SHORTLISTED) {
            throw new BusinessException("Không thể rút ứng tuyển ở trạng thái " + a.getStatus(),
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        a.setStatus(ApplicationStatus.WITHDRAWN);
        applicationRepository.save(a);
        return ProductMapper.toDto(a, kol);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductApplicationResponse> listMine(int page, int size) {
        KolProfile kol = currentKol();
        Page<ProductApplication> result = applicationRepository.findByKolProfileId(kol.getId(), pageable(page, size));
        return PageResponse.of(result.map(a -> ProductMapper.toDto(a, kol)));
    }

    // ---- Brand side ----------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<ProductApplicationResponse> listForProduct(Long productId, ApplicationStatus status,
                                                                   int page, int size) {
        productService.requireOwnedProduct(productId);
        Page<ProductApplication> result = (status == null)
                ? applicationRepository.findByProductId(productId, pageable(page, size))
                : applicationRepository.findByProductIdAndStatus(productId, status, pageable(page, size));
        return mapApplications(result);
    }

    /**
     * Ranks the still-selectable applicants (PENDING/SHORTLISTED) for a product and returns the
     * best {@code limit} of them by the chosen metric: {@code rating} (default), {@code reviews}
     * or {@code followers}. This is the "lọc top 5" feature.
     */
    @Transactional(readOnly = true)
    public List<ProductApplicationResponse> topApplicants(Long productId, String by, int limit) {
        productService.requireOwnedProduct(productId);
        int max = (limit <= 0 || limit > 50) ? 5 : limit;

        List<ProductApplication> candidates = applicationRepository.findByProductId(productId).stream()
                .filter(a -> a.getStatus() == ApplicationStatus.PENDING
                        || a.getStatus() == ApplicationStatus.SHORTLISTED)
                .toList();
        if (candidates.isEmpty()) {
            return List.of();
        }
        Map<Long, KolProfile> kols = loadKols(candidates);
        return candidates.stream()
                .sorted(rankingComparator(by, kols))
                .limit(max)
                .map(a -> ProductMapper.toDto(a, kols.get(a.getKolProfileId())))
                .toList();
    }

    @Transactional
    public ProductApplicationResponse shortlist(Long applicationId) {
        ProductApplication a = getApplication(applicationId);
        productService.requireOwnedProduct(a.getProductId());
        if (a.getStatus() != ApplicationStatus.PENDING) {
            throw new BusinessException("Chỉ shortlist được ứng tuyển đang PENDING",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        a.setStatus(ApplicationStatus.SHORTLISTED);
        applicationRepository.save(a);
        KolProfile kol = kolProfileRepository.findById(a.getKolProfileId()).orElse(null);
        notifyKol(kol, NotificationType.APPLICATION_SHORTLISTED,
                "Bạn được vào shortlist",
                "Bạn đã được brand đưa vào shortlist cho một sản phẩm.",
                "/applications/mine");
        return ProductMapper.toDto(a, kol);
    }

    @Transactional
    public ProductApplicationResponse reject(Long applicationId, String reason) {
        ProductApplication a = getApplication(applicationId);
        productService.requireOwnedProduct(a.getProductId());
        if (a.getStatus() == ApplicationStatus.ACCEPTED
                || a.getStatus() == ApplicationStatus.REJECTED
                || a.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new BusinessException("Không thể từ chối ứng tuyển ở trạng thái " + a.getStatus(),
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        a.setStatus(ApplicationStatus.REJECTED);
        a.setRejectReason(reason);
        applicationRepository.save(a);
        KolProfile kol = kolProfileRepository.findById(a.getKolProfileId()).orElse(null);
        notifyKol(kol, NotificationType.APPLICATION_REJECTED,
                "Ứng tuyển bị từ chối",
                "Rất tiếc, ứng tuyển của bạn đã bị từ chối." + (reason == null ? "" : " Lý do: " + reason),
                "/applications/mine");
        return ProductMapper.toDto(a, kol);
    }

    /** Accepts an applicant → creates a PENDING booking and links it back to the application. */
    @Transactional
    public ProductApplicationResponse accept(Long applicationId) {
        ProductApplication a = getApplication(applicationId);
        Product product = productService.requireOwnedProduct(a.getProductId());
        if (a.getStatus() != ApplicationStatus.PENDING && a.getStatus() != ApplicationStatus.SHORTLISTED) {
            throw new BusinessException("Chỉ duyệt được ứng tuyển PENDING/SHORTLISTED",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        if (a.getBookingId() != null) {
            throw new BusinessException("Ứng tuyển đã được duyệt", ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        KolProfile kol = kolProfileRepository.findById(a.getKolProfileId())
                .orElseThrow(() -> ResourceNotFoundException.of("KolProfile", a.getKolProfileId()));
        if (kol.getStatus() != KolProfileStatus.APPROVED) {
            throw new BusinessException("Hồ sơ KOL không còn hợp lệ để tạo booking",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }

        BigDecimal budget = a.getProposedPrice() != null ? a.getProposedPrice() : product.getBudget();
        if (budget == null || budget.signum() <= 0) {
            throw new BusinessException(
                    "Cần đặt ngân sách sản phẩm hoặc giá đề xuất (> 0) trước khi duyệt",
                    ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        }

        Booking booking = bookingService.createBookingFromApplication(
                product.getBrandProfileId(), kol.getId(),
                product.getTitle(), product.getDescription(), null,
                budget, null, product.getDeadline());

        a.setStatus(ApplicationStatus.ACCEPTED);
        a.setBookingId(booking.getId());
        applicationRepository.save(a);

        notifyKol(kol, NotificationType.APPLICATION_ACCEPTED,
                "Ứng tuyển được duyệt",
                "Brand đã duyệt bạn cho sản phẩm \"" + product.getTitle() + "\". Một booking đã được tạo.",
                "/bookings/" + booking.getId());

        // Close the posting once all slots are filled.
        long accepted = applicationRepository.countByProductIdAndStatus(product.getId(), ApplicationStatus.ACCEPTED);
        if (accepted >= product.getSlots()) {
            product.setStatus(ProductStatus.CLOSED);
            productRepository.save(product);
            log.info("Product {} closed: all {} slot(s) filled", product.getId(), product.getSlots());
        }

        log.info("Application {} accepted; booking {} created", applicationId, booking.getId());
        return ProductMapper.toDto(a, kol);
    }

    // ---- Helpers -------------------------------------------------------------------------------

    private KolProfile currentKol() {
        if (SecurityUtils.currentRole() != Role.KOL) {
            throw new BusinessException("Chỉ KOL mới có thể thực hiện thao tác này",
                    ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        return kolProfileService.getByUserId(SecurityUtils.currentUserId());
    }

    private ProductApplication getApplication(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ProductApplication", id));
    }

    private Map<Long, KolProfile> loadKols(List<ProductApplication> applications) {
        Set<Long> ids = applications.stream()
                .map(ProductApplication::getKolProfileId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return kolProfileRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(KolProfile::getId, k -> k));
    }

    private PageResponse<ProductApplicationResponse> mapApplications(Page<ProductApplication> page) {
        Map<Long, KolProfile> kols = loadKols(page.getContent());
        return PageResponse.of(page.map(a -> ProductMapper.toDto(a, kols.get(a.getKolProfileId()))));
    }

    private static Comparator<ProductApplication> rankingComparator(String by, Map<Long, KolProfile> kols) {
        Comparator<ProductApplication> byRating =
                Comparator.comparing((ProductApplication a) -> rating(kols.get(a.getKolProfileId())));
        Comparator<ProductApplication> byReviews =
                Comparator.comparingInt((ProductApplication a) -> reviews(kols.get(a.getKolProfileId())));
        Comparator<ProductApplication> byFollowers =
                Comparator.comparingLong((ProductApplication a) -> followers(kols.get(a.getKolProfileId())));

        String metric = by == null ? "rating" : by.trim().toLowerCase();
        Comparator<ProductApplication> ascending = switch (metric) {
            case "followers" -> byFollowers.thenComparing(byReviews).thenComparing(byRating);
            case "reviews" -> byReviews.thenComparing(byRating).thenComparing(byFollowers);
            default -> byRating.thenComparing(byReviews).thenComparing(byFollowers);
        };
        return ascending.reversed(); // best first
    }

    private static BigDecimal rating(KolProfile k) {
        return k == null || k.getAvgRating() == null ? BigDecimal.ZERO : k.getAvgRating();
    }

    private static int reviews(KolProfile k) {
        return k == null || k.getReviewCount() == null ? 0 : k.getReviewCount();
    }

    private static long followers(KolProfile k) {
        return k == null || k.getMaxFollowerCount() == null ? 0L : k.getMaxFollowerCount();
    }

    private void notifyKol(KolProfile kol, NotificationType type, String title, String message, String link) {
        if (kol != null) {
            notifyUser(kol.getUserId(), type, title, message, link);
        }
    }

    private void notifyUser(Long userId, NotificationType type, String title, String message, String link) {
        if (userId == null) {
            return;
        }
        notificationService.send(userId, type, title, message, link);
        userRepository.findById(userId).map(AppUser::getEmail)
                .ifPresent(email -> emailService.sendNotification(email, title, message, link));
    }

    private static PageRequest pageable(int page, int size) {
        if (size <= 0 || size > 100) size = 20;
        if (page < 0) page = 0;
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
