package com.ia.app.web;

import com.ia.app.dto.TipoEntidadeCampoRegraRequest;
import com.ia.app.dto.TipoEntidadeCampoRegraResponse;
import com.ia.app.mapper.TipoEntidadeCampoRegraMapper;
import com.ia.app.service.TipoEntidadeCampoRegraService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tipos-entidade")
public class TipoEntidadeCampoRegraController {

  private final TipoEntidadeCampoRegraService service;

  public TipoEntidadeCampoRegraController(TipoEntidadeCampoRegraService service) {
    this.service = service;
  }

  @GetMapping("/{id}/campos")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<List<TipoEntidadeCampoRegraResponse>> list(@PathVariable Long id) {
    return ResponseEntity.ok(service.list(id).stream()
      .map(TipoEntidadeCampoRegraMapper::toResponse)
      .toList());
  }

  @PutMapping("/{id}/campos")
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<List<TipoEntidadeCampoRegraResponse>> save(
      @PathVariable Long id,
      @Valid @RequestBody List<TipoEntidadeCampoRegraRequest> request) {
    return ResponseEntity.ok(service.saveAll(id, request).stream()
      .map(TipoEntidadeCampoRegraMapper::toResponse)
      .toList());
  }
}
