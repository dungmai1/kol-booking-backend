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
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "r2")
public class R2StorageService implements StorageService {

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
        UploadFilePolicy.ValidatedUpload validated = UploadFilePolicy.validate(file);
        String contentType = validated.contentType();

        try {
            String ext = UploadFilePolicy.extractExtension(file.getOriginalFilename());
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
}
