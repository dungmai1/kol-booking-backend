package kolbooking.datn.common.storage;

import jakarta.annotation.PostConstruct;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class FileStorageService implements StorageService {

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
        UploadFilePolicy.validate(file);

        try {
            String ext = UploadFilePolicy.extractExtension(file.getOriginalFilename());
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
}
