package kolbooking.datn.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final EmailVerificationInterceptor emailVerificationInterceptor;

    @Value("${app.storage.provider:local}")
    private String storageProvider;

    @Value("${app.storage.local.root:uploads}")
    private String rootDir;

    @Value("${app.storage.public-url-prefix:/uploads}")
    private String publicUrlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if ("local".equals(storageProvider)) {
            String location = "file:" + Paths.get(rootDir).toAbsolutePath().toString().replace('\\', '/') + "/";
            registry.addResourceHandler(publicUrlPrefix + "/**").addResourceLocations(location);
        }
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(emailVerificationInterceptor).addPathPatterns("/api/**");
    }
}
