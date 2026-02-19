package com.ia.app.web;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.dto.MovimentoItemTipoRequest;
import com.ia.app.dto.MovimentoItemTipoResponse;
import com.ia.app.service.MovimentoConfigFeatureToggle;
import com.ia.app.service.MovimentoItemTipoService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/movimentos/configuracoes/tipos-itens")
public class MovimentoItemTipoController {

  private final MovimentoItemTipoService service;
  private final MovimentoConfigFeatureToggle featureToggle;

  public MovimentoItemTipoController(MovimentoItemTipoService service, MovimentoConfigFeatureToggle featureToggle) {
    this.service = service;
    this.featureToggle = featureToggle;
  }

  @GetMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('MOVIMENTO_ITEM_CONFIGURAR')")
  public ResponseEntity<Page<MovimentoItemTipoResponse>> list(
      @RequestParam(required = false) String nome,
      @RequestParam(required = false) CatalogConfigurationType catalogType,
      @RequestParam(required = false) Boolean ativo,
      Pageable pageable) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(service.list(nome, catalogType, ativo, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('MOVIMENTO_ITEM_CONFIGURAR')")
  public ResponseEntity<MovimentoItemTipoResponse> getById(@PathVariable Long id) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(service.getById(id));
  }

  @PostMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('MOVIMENTO_ITEM_CONFIGURAR')")
  public ResponseEntity<MovimentoItemTipoResponse> create(@Valid @RequestBody MovimentoItemTipoRequest request) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(service.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('MOVIMENTO_ITEM_CONFIGURAR')")
  public ResponseEntity<MovimentoItemTipoResponse> update(
      @PathVariable Long id,
      @Valid @RequestBody MovimentoItemTipoRequest request) {
    featureToggle.assertEnabled();
    return ResponseEntity.ok(service.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('MOVIMENTO_ITEM_CONFIGURAR')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    featureToggle.assertEnabled();
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
