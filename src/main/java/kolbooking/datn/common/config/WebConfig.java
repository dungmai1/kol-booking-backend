package kolbooking.datn.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.storage.local.root:uploads}")
    private String rootDir;

    @Value("${app.storage.public-url-prefix:/uploads}")
    private String publicUrlPrefix;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = "file:" + Paths.get(rootDir).toAbsolutePath().toString().replace('\\', '/') + "/";
        registry.addResourceHandler(publicUrlPrefix + "/**").addResourceLocations(location);
    }
}
