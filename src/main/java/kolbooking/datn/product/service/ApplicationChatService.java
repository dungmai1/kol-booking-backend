package kolbooking.datn.product.service;

import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.brand.service.BrandProfileService;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.kol.service.KolProfileService;
import kolbooking.datn.notification.domain.NotificationType;
import kolbooking.datn.notification.service.NotificationService;
import kolbooking.datn.product.domain.ApplicationMessage;
import kolbooking.datn.product.domain.ApplicationStatus;
import kolbooking.datn.product.domain.Product;
import kolbooking.datn.product.domain.ProductApplication;
import kolbooking.datn.product.dto.ApplicationMessageResponse;
import kolbooking.datn.product.repository.ApplicationMessageRepository;
import kolbooking.datn.product.repository.ProductApplicationRepository;
import kolbooking.datn.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationChatService {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final ApplicationMessageRepository messageRepository;
    private final ProductApplicationRepository applicationRepository;
    private final ProductRepository productRepository;
    private final KolProfileService kolProfileService;
    private final BrandProfileService brandProfileService;
    private final ApplicationChatSseRegistry chatSseRegistry;
    private final NotificationService notificationService;
    private final PlatformTransactionManager txManager;

    // ---- SSE -------------------------------------------------------------------------------

    public SseEmitter connectStream(Long applicationId) {
        requireAccess(applicationId); // validates KOL/Brand ownership; terminal apps can still stream (read-only)
        return chatSseRegistry.connect(applicationId, SSE_TIMEOUT_MS);
    }

    // ---- Send message ----------------------------------------------------------------------

    @Transactional
    public ApplicationMessageResponse sendMessage(Long applicationId, String rawContent) {
        ProductApplication app = requireAccess(applicationId);
        if (ApplicationStatus.TERMINAL.contains(app.getStatus())) {
            throw new BusinessException(
                    "Ứng tuyển đã kết thúc, không thể gửi tin nhắn mới",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }

        String content = rawContent == null ? "" : rawContent.trim();
        if (content.isEmpty()) {
            throw new BusinessException("Nội dung tin nhắn không được để trống",
                    ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        }
        if (content.length() > 5000) {
            throw new BusinessException("Tin nhắn không vượt quá 5000 ký tự",
                    ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        }

        Role role = SecurityUtils.currentRole();
        ApplicationMessage msg = ApplicationMessage.builder()
                .applicationId(applicationId)
                .senderUserId(SecurityUtils.currentUserId())
                .senderRole(role.name())
                .content(content)
                .build();
        msg = messageRepository.save(msg);

        ApplicationMessageResponse response = ApplicationMessageResponse.from(msg);

        // Resolve recipient notification target before the transaction closes
        final Long recipientUserId = resolveRecipientUserId(app, role);
        final String productTitle = productRepository.findById(app.getProductId())
                .map(Product::getTitle).orElse("sản phẩm");

        // Push SSE and in-app notification after commit so recipient sees committed data
        final TransactionTemplate newTx = new TransactionTemplate(txManager);
        newTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chatSseRegistry.push(applicationId, response);
                if (recipientUserId != null) {
                    newTx.execute(status -> {
                        notificationService.send(
                                recipientUserId,
                                NotificationType.NEW_MESSAGE,
                                "Tin nhắn thương lượng mới",
                                "Bạn có tin nhắn mới trong ứng tuyển \"" + productTitle + "\"",
                                "/applications/" + applicationId + "/chat");
                        return null;
                    });
                }
            }
        });

        log.info("Application {} chat: {} sent message {}", applicationId, role, msg.getId());
        return response;
    }

    // ---- List messages ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<ApplicationMessageResponse> listMessages(Long applicationId, int page, int size) {
        requireAccess(applicationId); // validates ownership; terminal apps retain read access
        if (size <= 0 || size > 200) size = 50;
        if (page < 0) page = 0;
        Page<ApplicationMessage> result = messageRepository.findByApplicationId(
                applicationId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(result.map(ApplicationMessageResponse::from));
    }

    // ---- Helpers ---------------------------------------------------------------------------

    private Long resolveRecipientUserId(ProductApplication app, Role senderRole) {
        try {
            if (senderRole == Role.BRAND) {
                return kolProfileService.getById(app.getKolProfileId()).getUserId();
            } else {
                Product product = productRepository.findById(app.getProductId()).orElse(null);
                if (product == null) return null;
                return brandProfileService.getById(product.getBrandProfileId()).getUserId();
            }
        } catch (Exception e) {
            log.warn("Could not resolve notification recipient for application {}: {}", app.getId(), e.getMessage());
            return null;
        }
    }

    // ---- Access control --------------------------------------------------------------------

    /**
     * Validates the current user is either the KOL who made the application or the Brand who
     * owns the product. Terminal applications (WITHDRAWN/REJECTED) can still be read for chat
     * history but no new messages can be sent — that check is in {@link #sendMessage}.
     *
     * @return the validated application entity (already fetched, no second DB call needed)
     */
    private ProductApplication requireAccess(Long applicationId) {
        ProductApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProductApplication", applicationId));

        Long userId = SecurityUtils.currentUserId();
        Role role = SecurityUtils.currentRole();

        if (role == Role.KOL) {
            Long kolId = kolProfileService.getByUserId(userId).getId();
            if (!kolId.equals(app.getKolProfileId())) {
                throw new BusinessException("Không phải ứng tuyển của bạn",
                        ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
            }
        } else if (role == Role.BRAND) {
            Long brandId = brandProfileService.getByUserId(userId).getId();
            Product product = productRepository.findById(app.getProductId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Product", app.getProductId()));
            if (!brandId.equals(product.getBrandProfileId())) {
                throw new BusinessException("Không phải sản phẩm của bạn",
                        ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
            }
        } else {
            throw new BusinessException("Không có quyền truy cập",
                    ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        return app;
    }
}
