package com.griya.learn.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebResourceConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebResourceConfig.class);

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File dir = resolveDir();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String path = dir.getAbsolutePath().replace("\\", "/");
        log.info("头像存储目录: {}", path);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + path + "/");
    }

    public static File resolveDir(String relativePath) {
        File dir = new File(relativePath);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), relativePath);
        }
        return dir;
    }

    private File resolveDir() {
        return resolveDir(uploadDir);
    }
}
