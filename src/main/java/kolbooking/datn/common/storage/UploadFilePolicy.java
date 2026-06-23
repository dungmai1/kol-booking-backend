package kolbooking.datn.common.storage;

import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Set;

/** Shared MIME / size rules for {@link StorageService} implementations. */
public final class UploadFilePolicy {

    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    public static final long MAX_VIDEO_BYTES = 100L * 1024 * 1024;
    public static final long MAX_DOCUMENT_BYTES = 10L * 1024 * 1024;

    private static final Set<String> ALLOWED_IMAGE = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> ALLOWED_VIDEO = Set.of("video/mp4");
    private static final Set<String> ALLOWED_DOCUMENT = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private static final Map<String, String> EXTENSION_TO_MIME = Map.ofEntries(
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".png", "image/png"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".webp", "image/webp"),
            Map.entry(".mp4", "video/mp4"),
            Map.entry(".pdf", "application/pdf"),
            Map.entry(".doc", "application/msword"),
            Map.entry(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    );

    public enum Category {
        IMAGE, VIDEO, DOCUMENT
    }

    private UploadFilePolicy() {}

    public record ValidatedUpload(Category category, String contentType) {}

    public static ValidatedUpload validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is empty", ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        }
        String contentType = resolveContentType(file);
        Category category = classify(contentType);
        if (category == null) {
            throw new BusinessException(
                    "Unsupported content type: " + contentType
                            + " (allowed: JPEG, PNG, GIF, WEBP, MP4, PDF, DOC, DOCX)",
                    ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        }
        long maxBytes = maxBytes(category);
        if (file.getSize() > maxBytes) {
            long maxMb = maxBytes / (1024 * 1024);
            throw new BusinessException("File exceeds size limit of " + maxMb + " MB",
                    ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        }
        return new ValidatedUpload(category, contentType);
    }

    public static long maxBytes(Category category) {
        return switch (category) {
            case IMAGE -> MAX_IMAGE_BYTES;
            case VIDEO -> MAX_VIDEO_BYTES;
            case DOCUMENT -> MAX_DOCUMENT_BYTES;
        };
    }

    private static Category classify(String contentType) {
        if (contentType == null) return null;
        if (ALLOWED_IMAGE.contains(contentType)) return Category.IMAGE;
        if (ALLOWED_VIDEO.contains(contentType)) return Category.VIDEO;
        if (ALLOWED_DOCUMENT.contains(contentType)) return Category.DOCUMENT;
        return null;
    }

    public static String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && classify(contentType) != null) {
            return contentType;
        }
        String ext = extractExtension(file.getOriginalFilename());
        String inferred = EXTENSION_TO_MIME.get(ext);
        return inferred != null ? inferred : contentType;
    }

    public static String extractExtension(String originalFilename) {
        if (originalFilename == null) return "";
        int idx = originalFilename.lastIndexOf('.');
        if (idx < 0) return "";
        String ext = originalFilename.substring(idx).toLowerCase();
        if (!ext.matches("^\\.[a-z0-9]{1,10}$")) return "";
        return ext;
    }
}
