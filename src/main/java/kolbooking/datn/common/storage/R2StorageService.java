package kolbooking.datn.common.storage;

import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "r2")
public class R2StorageService implements StorageService {

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES = 100L * 1024 * 1024;
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

    private final S3Client s3Client;
    private final String bucket;
    private final String publicUrl;

    public R2StorageService(
            @Value("${app.storage.r2.account-id}") String accountId,
            @Value("${app.storage.r2.access-key-id}") String accessKeyId,
            @Value("${app.storage.r2.secret-access-key}") String secretAccessKey,
            @Value("${app.storage.r2.bucket}") String bucket,
            @Value("${app.storage.r2.public-url}") String publicUrl
    ) {
        this.bucket = bucket;
        this.publicUrl = publicUrl.replaceAll("/+$", "");

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create("https://" + accountId + ".r2.cloudflarestorage.com"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .region(Region.of("auto"))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .httpClient(UrlConnectionHttpClient.builder().build())
                .build();

        log.info("R2 storage initialised: bucket={}, publicUrl={}", bucket, this.publicUrl);
    }

    @Override
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
            String key = String.format("%d/%02d/%s%s",
                    today.getYear(),
                    today.getMonthValue(),
                    UUID.randomUUID().toString().replace("-", ""),
                    ext);

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return publicUrl + "/" + key;
        } catch (IOException | SdkException ex) {
            log.error("Failed to upload file to R2", ex);
            throw new BusinessException("Failed to upload file",
                    ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && (ALLOWED_IMAGE.contains(contentType) || ALLOWED_VIDEO.contains(contentType))) {
            return contentType;
        }
        String ext = extractExtension(file.getOriginalFilename());
        String inferred = EXTENSION_TO_MIME.get(ext);
        return inferred != null ? inferred : contentType;
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
