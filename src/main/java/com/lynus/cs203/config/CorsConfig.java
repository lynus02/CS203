package com.lynus.cs203.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("Configuring CORS settings");

        registry.addMapping("/**")
                .allowedOrigins("https://cs203-frontend-production.up.railway.app",
                        "http://localhost:5173", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);

        log.info("CORS configured for origins: http://localhost:5173, http://localhost:3000");
        log.debug("CORS details - Methods: [GET, POST, PUT, DELETE, OPTIONS], Credentials: enabled, Max-Age: 3600s");
    }
}
