package com.ia.app.web;

import com.ia.app.domain.MovimentoTipo;
import com.ia.app.dto.MovimentoConfigDuplicarRequest;
import com.ia.app.dto.MovimentoConfigItemTipoRequest;
import com.ia.app.dto.MovimentoConfigItemTipoResponse;
import com.ia.app.dto.MovimentoConfigCoverageWarningResponse;
import com.ia.app.dto.MovimentoConfigRequest;
import com.ia.app.dto.MovimentoConfigResolverResponse;
import com.ia.app.dto.MovimentoConfigResponse;
import com.ia.app.dto.MovimentoTipoResponse;
import com.ia.app.service.MovimentoConfigFeatureToggle;
import com.ia.app.service.MovimentoConfigItemTipoService;
import com.ia.app.service.MovimentoConfigService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/movimentos/configuracoes")
public class MovimentoConfigController {

  private final MovimentoConfigService service;
  private final MovimentoConfigItemTipoService itemTipoService;
  private final MovimentoConfigFeatureToggle featureToggle;

  public MovimentoConfigController(
      MovimentoConfigService service,
      MovimentoConfigItemTipoService itemTipoService,
      MovimentoConfigFeatureToggle featureToggle) {
    this.service = service;
    this.itemTipoService = itemTipoService;
    this.featureToggle = featureToggle;
  }

  @GetMapping("/tipos")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<List<MovimentoTipoResponse>> listTipos() {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(service.listTipos());
  }

  @GetMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<Page<MovimentoConfigResponse>> listByTipo(
      @RequestParam String tipo,
      Pageable pageable) {
    featureToggle.assertEnabled();
    MovimentoTipo tipoMovimento = MovimentoTipo.from(tipo);
    return ResponseEntity.ok(service.listByTipo(tipoMovimento, pageable));
  }

  @GetMapping("/warnings")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<List<MovimentoConfigCoverageWarningResponse>> listCoverageWarnings() {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(service.listCoverageWarnings());
  }

  @GetMapping("/menu")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<MovimentoTipoResponse>> listMenuByEmpresa(
      @RequestParam Long empresaId) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(service.listMenuTiposForEmpresa(empresaId));
  }

  @GetMapping("/resolver")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<MovimentoConfigResolverResponse> resolve(
      @RequestParam String tipo,
      @RequestParam Long empresaId,
      @RequestParam(required = false) String contextoKey) {
    featureToggle.assertEnabled();
    MovimentoTipo tipoMovimento = MovimentoTipo.from(tipo);
    return ResponseEntity.ok(service.resolve(tipoMovimento, empresaId, contextoKey));
  }

  @GetMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<MovimentoConfigResponse> getById(@PathVariable Long id) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(service.getById(id));
  }

  @PostMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<MovimentoConfigResponse> create(
      @Valid @RequestBody MovimentoConfigRequest request) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(service.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<MovimentoConfigResponse> update(
      @PathVariable Long id,
      @Valid @RequestBody MovimentoConfigRequest request) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(service.update(id, request));
  }

  @PostMapping("/{id}/duplicar")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<MovimentoConfigResponse> duplicar(
      @PathVariable Long id,
      @RequestBody(required = false) @Valid MovimentoConfigDuplicarRequest request) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(service.duplicar(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('CONFIG_EDITOR')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    featureToggle.assertEnabled();
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/tipos-itens")
  @PreAuthorize("@permissaoGuard.hasPermissao('MOVIMENTO_ITEM_CONFIGURAR')")
  public ResponseEntity<List<MovimentoConfigItemTipoResponse>> listTiposItensByConfig(@PathVariable Long id) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(itemTipoService.listByConfig(id));
  }

  @PutMapping("/{id}/tipos-itens")
  @PreAuthorize("@permissaoGuard.hasPermissao('MOVIMENTO_ITEM_CONFIGURAR')")
  public ResponseEntity<List<MovimentoConfigItemTipoResponse>> replaceTiposItensByConfig(
      @PathVariable Long id,
      @RequestBody List<MovimentoConfigItemTipoRequest> request) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(itemTipoService.replaceByConfig(id, request));
  }
}
