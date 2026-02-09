package com.ia.app.web;

import com.ia.app.dto.CampoDefinicaoRequest;
import com.ia.app.dto.CampoDefinicaoResponse;
import com.ia.app.mapper.CampoDefinicaoMapper;
import com.ia.app.service.CampoDefinicaoService;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/campos-definicao")
public class CampoDefinicaoController {

  private final CampoDefinicaoService service;

  public CampoDefinicaoController(CampoDefinicaoService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<Page<CampoDefinicaoResponse>> list(
      @RequestParam Long tipoEntidadeId,
      @RequestHeader(value = "If-Modified-Since", required = false) String ifModifiedSince,
      @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
      Pageable pageable) {
    Instant maxUpdatedAt = service.maxUpdatedAt(tipoEntidadeId);
    String etag = maxUpdatedAt == null ? null : "\"" + Integer.toHexString(maxUpdatedAt.hashCode() + tipoEntidadeId.hashCode()) + "\"";
    if (etag != null && etag.equals(ifNoneMatch)) {
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
    }
    if (maxUpdatedAt != null && ifModifiedSince != null) {
      try {
        long since = java.time.ZonedDateTime.parse(ifModifiedSince, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
          .toInstant().toEpochMilli();
        if (maxUpdatedAt.toEpochMilli() <= since) {
          return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
      } catch (Exception ignored) {
      }
    }

    Page<CampoDefinicaoResponse> page = service.list(tipoEntidadeId, pageable)
      .map(CampoDefinicaoMapper::toResponse);
    ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
    if (maxUpdatedAt != null) {
      builder.lastModified(maxUpdatedAt.toEpochMilli());
    }
    if (etag != null) {
      builder.eTag(etag);
    }
    return builder.body(page);
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<CampoDefinicaoResponse> create(@Valid @RequestBody CampoDefinicaoRequest request) {
    return ResponseEntity.ok(CampoDefinicaoMapper.toResponse(service.create(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<CampoDefinicaoResponse> update(@PathVariable Long id,
      @Valid @RequestBody CampoDefinicaoRequest request) {
    return ResponseEntity.ok(CampoDefinicaoMapper.toResponse(service.update(id, request)));
  }
}
