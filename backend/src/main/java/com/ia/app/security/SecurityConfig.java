package com.ia.app.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, TenantAccessFilter tenantAccessFilter) throws Exception {
    http
      .csrf(csrf -> csrf.disable())
      .cors(Customizer.withDefaults())
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
        .requestMatchers("/actuator/health").permitAll()
        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/me").authenticated()
        .anyRequest().authenticated()
      )
      .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
      .addFilterAfter(tenantAccessFilter, BasicAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    String raw = System.getenv().getOrDefault("CORS_ALLOWED_ORIGINS",
      "http://localhost:8080,http://192.168.0.243:8080");
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(java.util.Arrays.stream(raw.split(","))
      .map(String::trim)
      .filter(s -> !s.isEmpty())
      .toList());
    config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(java.util.List.of("*"));
    config.setExposedHeaders(java.util.List.of("ETag", "Location"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
    return converter;
  }

  private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    Object realmAccess = jwt.getClaims().get("realm_access");
    if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
      return List.of();
    }
    Object rolesObj = realmAccessMap.get("roles");
    if (!(rolesObj instanceof Collection<?> roles)) {
      return List.of();
    }
    return roles.stream()
      .filter(String.class::isInstance)
      .map(String.class::cast)
      .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
      .collect(Collectors.toSet());
  }
}
