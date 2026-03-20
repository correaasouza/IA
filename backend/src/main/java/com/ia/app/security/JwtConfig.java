package com.ia.app.security;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtConfig {

  @Bean
  JwtDecoder jwtDecoder(
      @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
      @Value("${app.security.jwt.accepted-issuers:}") String acceptedIssuersRaw) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

    List<String> acceptedIssuers = Stream.concat(
            Stream.of(issuer),
            Arrays.stream(acceptedIssuersRaw.split(",")))
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .distinct()
        .toList();

    OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefault();
    OAuth2TokenValidator<Jwt> issuerValidator = jwt -> {
      String tokenIssuer = jwt.getIssuer() == null ? "" : jwt.getIssuer().toString();
      if (acceptedIssuers.contains(tokenIssuer)) {
        return OAuth2TokenValidatorResult.success();
      }
      OAuth2Error error = new OAuth2Error(
          "invalid_token",
          "The iss claim is not allowed",
          null);
      return OAuth2TokenValidatorResult.failure(error);
    };

    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidator, issuerValidator));
    return decoder;
  }
}
