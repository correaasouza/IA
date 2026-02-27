package com.ia.app.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
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
    Map<String, Object> user = new HashMap<>();
    user.put("username", username);
    user.put("enabled", enabled);
    user.put("emailVerified", true);
    if (email != null && !email.isBlank()) {
      user.put("email", email);
    }

    String location = webClient.post()
      .uri("/admin/realms/{realm}/users", targetRealm)
      .contentType(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer " + token)
      .bodyValue(user)
      .exchangeToMono(resp -> {
        if (resp.statusCode().is4xxClientError()) {
          return resp.bodyToMono(String.class)
            .defaultIfEmpty("")
            .flatMap(body -> reactor.core.publisher.Mono.error(
              new IllegalArgumentException(mapCreateUserError(body))
            ));
        }
        String loc = resp.headers().asHttpHeaders().getFirst("Location");
        if (loc == null) {
          return resp.bodyToMono(String.class)
            .defaultIfEmpty("")
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

  private String mapCreateUserError(String body) {
    String source = (body == null ? "" : body).toLowerCase();
    if (source.contains("same email")) {
      return "usuario_email_duplicado";
    }
    if (source.contains("same username")) {
      return "usuario_username_duplicado";
    }
    return "keycloak_create_user_failed: " + body;
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

  public boolean userExists(String userId) {
    if (userId == null || userId.isBlank()) {
      return false;
    }
    String token = getAdminToken();
    return Boolean.TRUE.equals(webClient.get()
      .uri("/admin/realms/{realm}/users/{id}", targetRealm, userId)
      .header("Authorization", "Bearer " + token)
      .exchangeToMono(resp -> {
        if (resp.statusCode().is2xxSuccessful()) {
          return reactor.core.publisher.Mono.just(true);
        }
        if (resp.statusCode().value() == 404) {
          return reactor.core.publisher.Mono.just(false);
        }
        return resp.bodyToMono(String.class)
          .defaultIfEmpty("")
          .flatMap(body -> reactor.core.publisher.Mono.error(
            new IllegalStateException("keycloak_user_lookup_failed: " + body)
          ));
      })
      .block(Duration.ofSeconds(10)));
  }

  public void deleteUser(String userId) {
    String token = getAdminToken();
    webClient.delete()
      .uri("/admin/realms/{realm}/users/{id}", targetRealm, userId)
      .header("Authorization", "Bearer " + token)
      .exchangeToMono(resp -> {
        if (resp.statusCode().value() == 404) {
          return reactor.core.publisher.Mono.empty();
        }
        if (resp.statusCode().isError()) {
          return resp.bodyToMono(String.class)
            .defaultIfEmpty("")
            .flatMap(body -> reactor.core.publisher.Mono.error(
              new IllegalStateException("keycloak_delete_user_failed: " + body)
            ));
        }
        return reactor.core.publisher.Mono.empty();
      })
      .block(Duration.ofSeconds(10));
  }

  private void assignRealmRoles(String userId, List<String> roles) {
    if (roles == null || roles.isEmpty()) {
      return;
    }
    String token = getAdminToken();
    List<Map<String, Object>> roleReps = new ArrayList<>();
    for (String roleName : roles) {
      String normalizedRole = roleName == null ? "" : roleName.trim();
      if (normalizedRole.isEmpty()) {
        continue;
      }
      Map<String, Object> role = fetchRealmRole(token, normalizedRole);
      if (role == null) {
        createRealmRole(token, normalizedRole);
        role = fetchRealmRole(token, normalizedRole);
      }
      if (role == null || role.get("id") == null || role.get("name") == null) {
        throw new IllegalStateException("keycloak_role_resolution_failed: " + normalizedRole);
      }
      roleReps.add(Map.of(
        "id", role.get("id"),
        "name", role.get("name")
      ));
    }
    if (roleReps.isEmpty()) {
      return;
    }

    webClient.post()
      .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", targetRealm, userId)
      .contentType(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer " + token)
      .bodyValue(roleReps)
      .retrieve()
      .toBodilessEntity()
      .block(Duration.ofSeconds(10));
  }

  private Map<String, Object> fetchRealmRole(String token, String roleName) {
    return webClient.get()
      .uri("/admin/realms/{realm}/roles/{role}", targetRealm, roleName)
      .header("Authorization", "Bearer " + token)
      .exchangeToMono(resp -> {
        if (resp.statusCode().is2xxSuccessful()) {
          return resp.bodyToMono(Map.class);
        }
        if (resp.statusCode().value() == 404) {
          return reactor.core.publisher.Mono.justOrEmpty(null);
        }
        return resp.bodyToMono(String.class)
          .defaultIfEmpty("")
          .flatMap(body -> reactor.core.publisher.Mono.error(
            new IllegalStateException("keycloak_role_lookup_failed: " + roleName + ": " + body)
          ));
      })
      .block(Duration.ofSeconds(10));
  }

  private void createRealmRole(String token, String roleName) {
    Map<String, Object> payload = Map.of("name", roleName);
    webClient.post()
      .uri("/admin/realms/{realm}/roles", targetRealm)
      .contentType(MediaType.APPLICATION_JSON)
      .header("Authorization", "Bearer " + token)
      .bodyValue(payload)
      .exchangeToMono(resp -> {
        if (resp.statusCode().is2xxSuccessful() || resp.statusCode().value() == 409) {
          return reactor.core.publisher.Mono.empty();
        }
        return resp.bodyToMono(String.class)
          .defaultIfEmpty("")
          .flatMap(body -> reactor.core.publisher.Mono.error(
            new IllegalStateException("keycloak_role_create_failed: " + roleName + ": " + body)
          ));
      })
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
