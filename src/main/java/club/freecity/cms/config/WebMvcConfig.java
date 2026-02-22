package club.freecity.cms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 获取上传目录的绝对路径
        String path = Paths.get(uploadPath).toAbsolutePath().toString().replace("\\", "/");
        if (!path.endsWith("/")) {
            path += "/";
        }
        String location = "file:" + path;
        
        // 2. 映射资源路径（例如 /uploads/** -> file:D:/Work/.../uploads/）
        registry.addResourceHandler("/" + uploadPath + "/**")
                .addResourceLocations(location);
    }
}
