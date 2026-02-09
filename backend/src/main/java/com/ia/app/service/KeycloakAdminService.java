package com.ia.app.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class KeycloakAdminService {

  private final WebClient webClient;
  private final String realm;
  private final String targetRealm;
  private final String adminRealm;
  private final String clientId;
  private final String username;
  private final String password;
  private final AtomicReference<TokenHolder> tokenHolder = new AtomicReference<>();

  public KeycloakAdminService(WebClient keycloakWebClient,
      @Value("${keycloak.admin.realm}") String realm,
      @Value("${keycloak.admin.target-realm}") String targetRealm,
      @Value("${keycloak.admin.admin-realm}") String adminRealm,
      @Value("${keycloak.admin.client-id}") String clientId,
      @Value("${keycloak.admin.username}") String username,
      @Value("${keycloak.admin.password}") String password) {
    this.webClient = keycloakWebClient;
    this.realm = realm;
    this.targetRealm = targetRealm;
    this.adminRealm = adminRealm;
    this.clientId = clientId;
    this.username = username;
    this.password = password;
  }

  public String createUser(String username, String email, String password, boolean enabled, List<String> roles) {
    String token = getAdminToken();
    Map<String, Object> user = Map.of(
      "username", username,
      "email", email,
      "enabled", enabled,
      "emailVerified", true
    );

    String location = webClient.post()
      .uri("/admin/realms/{realm}/users", targetRealm)
      .contentType(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer " + token)
      .bodyValue(user)
      .exchangeToMono(resp -> {
        String loc = resp.headers().asHttpHeaders().getFirst("Location");
        if (loc == null) {
          return resp.bodyToMono(String.class)
            .flatMap(body -> reactor.core.publisher.Mono.error(
              new RuntimeException("keycloak_create_user_failed: " + body)
            ));
        }
        return reactor.core.publisher.Mono.just(loc);
      })
      .block(Duration.ofSeconds(10));

    String userId = location.substring(location.lastIndexOf('/') + 1);

    setPassword(userId, password, false);
    assignRealmRoles(userId, roles);

    return userId;
  }

  public void setPassword(String userId, String password, boolean temporary) {
    String token = getAdminToken();
    Map<String, Object> body = Map.of(
      "type", "password",
      "value", password,
      "temporary", temporary
    );

    webClient.put()
      .uri("/admin/realms/{realm}/users/{id}/reset-password", targetRealm, userId)
      .contentType(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer " + token)
      .bodyValue(body)
      .retrieve()
      .toBodilessEntity()
      .block(Duration.ofSeconds(10));
  }

  public void disableUser(String userId) {
    String token = getAdminToken();
    Map<String, Object> body = Map.of("enabled", false);
    webClient.put()
      .uri("/admin/realms/{realm}/users/{id}", targetRealm, userId)
      .contentType(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer " + token)
      .bodyValue(body)
      .retrieve()
      .toBodilessEntity()
      .block(Duration.ofSeconds(10));
  }

  public void updateUser(String userId, Map<String, Object> fields) {
    if (fields == null || fields.isEmpty()) {
      return;
    }
    String token = getAdminToken();
    webClient.put()
      .uri("/admin/realms/{realm}/users/{id}", targetRealm, userId)
      .contentType(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer " + token)
      .bodyValue(fields)
      .retrieve()
      .toBodilessEntity()
      .block(Duration.ofSeconds(10));
  }

  public void deleteUser(String userId) {
    String token = getAdminToken();
    webClient.delete()
      .uri("/admin/realms/{realm}/users/{id}", targetRealm, userId)
      .header("Authorization", "Bearer " + token)
      .retrieve()
      .toBodilessEntity()
      .block(Duration.ofSeconds(10));
  }

  private void assignRealmRoles(String userId, List<String> roles) {
    if (roles == null || roles.isEmpty()) {
      return;
    }
    String token = getAdminToken();

    List<Map<String, Object>> roleReps = roles.stream().map(roleName -> {
      Map<String, Object> role = webClient.get()
        .uri("/admin/realms/{realm}/roles/{role}", targetRealm, roleName)
        .header("Authorization", "Bearer " + token)
        .retrieve()
        .bodyToMono(Map.class)
        .block(Duration.ofSeconds(10));
      return Map.of(
        "id", role.get("id"),
        "name", role.get("name")
      );
    }).toList();

    webClient.post()
      .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", targetRealm, userId)
      .contentType(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer " + token)
      .bodyValue(roleReps)
      .retrieve()
      .toBodilessEntity()
      .block(Duration.ofSeconds(10));
  }

  private String getAdminToken() {
    TokenHolder cached = tokenHolder.get();
    if (cached != null && !cached.isExpired()) {
      return cached.token();
    }

    Map<String, Object> tokenResponse = webClient.post()
      .uri("/realms/{realm}/protocol/openid-connect/token", adminRealm)
      .contentType(MediaType.APPLICATION_FORM_URLENCODED)
      .body(BodyInserters.fromFormData("client_id", clientId)
        .with("grant_type", "password")
        .with("username", username)
        .with("password", password))
      .retrieve()
      .bodyToMono(Map.class)
      .block(Duration.ofSeconds(10));

    String token = (String) tokenResponse.get("access_token");
    int expiresIn = (int) tokenResponse.getOrDefault("expires_in", 60);
    TokenHolder holder = new TokenHolder(token, System.currentTimeMillis() + (expiresIn - 10) * 1000L);
    tokenHolder.set(holder);
    return token;
  }

  private record TokenHolder(String token, long expiresAt) {
    boolean isExpired() {
      return System.currentTimeMillis() > expiresAt;
    }
  }
}
