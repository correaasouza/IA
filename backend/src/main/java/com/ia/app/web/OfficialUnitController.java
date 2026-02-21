package com.ia.app.web;

import com.ia.app.dto.OfficialUnitRequest;
import com.ia.app.dto.OfficialUnitResponse;
import com.ia.app.service.OfficialUnitService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/global/official-units")
public class OfficialUnitController {

  private final OfficialUnitService service;

  public OfficialUnitController(OfficialUnitService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("@globalScopeGuard.isMasterInMasterTenant()")
  public ResponseEntity<List<OfficialUnitResponse>> list(
      @RequestParam(required = false) Boolean ativo,
      @RequestParam(required = false) String text) {
    return ResponseEntity.ok(service.list(ativo, text));
  }

  @GetMapping("/{id}")
  @PreAuthorize("@globalScopeGuard.isMasterInMasterTenant()")
  public ResponseEntity<OfficialUnitResponse> get(@PathVariable UUID id) {
    return ResponseEntity.ok(service.get(id));
  }

  @PostMapping
  @PreAuthorize("@globalScopeGuard.isMasterInMasterTenant()")
  public ResponseEntity<OfficialUnitResponse> create(@Valid @RequestBody OfficialUnitRequest request) {
    return ResponseEntity.ok(service.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@globalScopeGuard.isMasterInMasterTenant()")
  public ResponseEntity<OfficialUnitResponse> update(
      @PathVariable UUID id,
      @Valid @RequestBody OfficialUnitRequest request) {
    return ResponseEntity.ok(service.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@globalScopeGuard.isMasterInMasterTenant()")
  public ResponseEntity<Void> delete(@PathVariable UUID id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
