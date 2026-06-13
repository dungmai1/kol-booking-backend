package kolbooking.datn.common.storage;

import jakarta.annotation.PostConstruct;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;    // 5 MB
    private static final long MAX_VIDEO_BYTES = 100L * 1024 * 1024;  // 100 MB
    private static final Set<String> ALLOWED_IMAGE = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> ALLOWED_VIDEO = Set.of("video/mp4");
    private static final Map<String, String> EXTENSION_TO_MIME = Map.of(
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png",
            ".gif", "image/gif",
            ".webp", "image/webp",
            ".mp4", "video/mp4"
    );

    private final Path rootDir;
    private final String publicUrlPrefix;

    public FileStorageService(
            @Value("${app.storage.local.root:uploads}") String rootDir,
            @Value("${app.storage.public-url-prefix:/uploads}") String publicUrlPrefix
    ) {
        this.rootDir = Paths.get(rootDir).toAbsolutePath();
        this.publicUrlPrefix = publicUrlPrefix;
    }

    @PostConstruct
    void init() throws IOException {
        Files.createDirectories(rootDir);
        log.info("File storage root = {}", rootDir);
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is empty", ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        }
        String contentType = resolveContentType(file);
        boolean isImage = ALLOWED_IMAGE.contains(contentType);
        boolean isVideo = ALLOWED_VIDEO.contains(contentType);
        if (!isImage && !isVideo) {
            throw new BusinessException("Unsupported content type: " + contentType
                            + " (allowed images: JPEG, PNG, GIF, WEBP; video: MP4)",
                    ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        }
        long maxBytes = isImage ? MAX_IMAGE_BYTES : MAX_VIDEO_BYTES;
        if (file.getSize() > maxBytes) {
            long maxMb = maxBytes / (1024 * 1024);
            throw new BusinessException("File exceeds size limit of " + maxMb + " MB",
                    ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        }

        try {
            String ext = extractExtension(file.getOriginalFilename());
            LocalDate today = LocalDate.now();
            Path dir = rootDir.resolve(String.valueOf(today.getYear()))
                    .resolve(String.format("%02d", today.getMonthValue()));
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            String relative = rootDir.relativize(target).toString().replace('\\', '/');
            return publicUrlPrefix + "/" + relative;
        } catch (IOException ex) {
            log.error("Failed to store file", ex);
            throw new BusinessException("Failed to store file",
                    ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && ALLOWED_IMAGE.contains(contentType)) {
            return contentType;
        }
        if (contentType != null && ALLOWED_VIDEO.contains(contentType)) {
            return contentType;
        }
        String ext = extractExtension(file.getOriginalFilename());
        String inferred = EXTENSION_TO_MIME.get(ext);
        if (inferred != null) {
            return inferred;
        }
        return contentType;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) return "";
        int idx = originalFilename.lastIndexOf('.');
        if (idx < 0) return "";
        String ext = originalFilename.substring(idx).toLowerCase();
        if (!ext.matches("^\\.[a-z0-9]{1,10}$")) return "";
        return ext;
    }
}
