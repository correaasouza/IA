package com.ia.app.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;

@Configuration
@EnableCaching
public class CacheConfig {

  @Bean
  CacheManager cacheManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager(
      "configColuna",
      "configFormulario",
      "permissoesUsuario",
      "papeisUsuario"
    );
    manager.setCaffeine(Caffeine.newBuilder()
      .expireAfterWrite(Duration.ofMinutes(5))
      .maximumSize(5000));
    return manager;
  }
}
