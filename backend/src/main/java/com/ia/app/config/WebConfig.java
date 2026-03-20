package com.ia.app.config;

import java.util.Arrays;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    String raw = System.getenv().getOrDefault("CORS_ALLOWED_ORIGINS",
      "http://localhost:4201,http://localhost:15000,http://host.docker.internal:15000,http://localhost:5000,http://host.docker.internal:5000,http://localhost:8080,http://host.docker.internal:8080");
    registry.addMapping("/api/**")
      .allowedOrigins(Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toArray(String[]::new))
      .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
      .allowedHeaders("*")
      .allowCredentials(true);
  }
}
