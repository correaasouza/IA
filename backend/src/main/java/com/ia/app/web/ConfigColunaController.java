package com.ia.app.web;

import com.ia.app.dto.ConfigRequest;
import com.ia.app.dto.ConfigResponse;
import com.ia.app.service.ConfigColunaService;
import com.ia.app.tenant.TenantContext;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config/colunas")
public class ConfigColunaController {

  private final ConfigColunaService service;

  public ConfigColunaController(ConfigColunaService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<ConfigResponse> resolve(
      @RequestParam String screenId,
      @RequestParam(required = false) String userId,
      @RequestParam(required = false) String roles,
      @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
    Long tenantId = TenantContext.getTenantId();
    List<String> roleList = roles == null ? List.of() : Arrays.asList(roles.split(","));
    String rolesKey = String.join("|", roleList);
    var maxUpdatedAt = service.maxUpdatedAt(tenantId, screenId);
    String etag = maxUpdatedAt == null ? null : "\"" + Integer.toHexString(maxUpdatedAt.hashCode()) + "\"";
    if (etag != null && etag.equals(ifNoneMatch)) {
      return ResponseEntity.status(304).eTag(etag).build();
    }
    var response = service.resolve(tenantId, screenId, userId, rolesKey, roleList);
    ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
    if (etag != null) {
      builder.eTag(etag);
    }
    if (maxUpdatedAt != null) {
      builder.lastModified(maxUpdatedAt.toEpochMilli());
    }
    return builder.body(response);
  }

  @PostMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<Void> save(@Valid @RequestBody ConfigRequest request) {
    Long tenantId = TenantContext.getTenantId();
    service.save(request, tenantId);
    return ResponseEntity.noContent().build();
  }
}
