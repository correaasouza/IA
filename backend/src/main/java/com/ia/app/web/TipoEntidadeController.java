package com.ia.app.web;

import com.ia.app.dto.TipoEntidadeConfigAgrupadorRequest;
import com.ia.app.dto.TipoEntidadeConfigAgrupadorResponse;
import com.ia.app.dto.TipoEntidadeRequest;
import com.ia.app.dto.TipoEntidadeResponse;
import com.ia.app.service.TipoEntidadeConfigPorAgrupadorService;
import com.ia.app.service.TipoEntidadeService;
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
@RequestMapping("/api/tipos-entidade")
public class TipoEntidadeController {

  private final TipoEntidadeService service;
  private final TipoEntidadeConfigPorAgrupadorService configPorAgrupadorService;

  public TipoEntidadeController(
      TipoEntidadeService service,
      TipoEntidadeConfigPorAgrupadorService configPorAgrupadorService) {
    this.service = service;
    this.configPorAgrupadorService = configPorAgrupadorService;
  }

  @GetMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<Page<TipoEntidadeResponse>> list(
      @RequestParam(required = false) String nome,
      @RequestParam(required = false) Boolean ativo,
      Pageable pageable) {
    return ResponseEntity.ok(service.list(nome, ativo, pageable).map(this::toResponse));
  }

  @GetMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<TipoEntidadeResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(toResponse(service.get(id)));
  }

  @PostMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<TipoEntidadeResponse> create(@Valid @RequestBody TipoEntidadeRequest request) {
    return ResponseEntity.ok(toResponse(service.create(request)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<TipoEntidadeResponse> update(
      @PathVariable Long id,
      @Valid @RequestBody TipoEntidadeRequest request) {
    return ResponseEntity.ok(toResponse(service.update(id, request)));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/config-agrupadores")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<List<TipoEntidadeConfigAgrupadorResponse>> listConfigPorAgrupador(@PathVariable Long id) {
    return ResponseEntity.ok(configPorAgrupadorService.listar(id));
  }

  @PutMapping("/{id}/config-agrupadores/{agrupadorId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<TipoEntidadeConfigAgrupadorResponse> updateConfigPorAgrupador(
      @PathVariable Long id,
      @PathVariable Long agrupadorId,
      @Valid @RequestBody TipoEntidadeConfigAgrupadorRequest request) {
    return ResponseEntity.ok(
      configPorAgrupadorService.atualizar(id, agrupadorId, Boolean.TRUE.equals(request.obrigarUmTelefone())));
  }

  private TipoEntidadeResponse toResponse(com.ia.app.domain.TipoEntidade tipoEntidade) {
    return new TipoEntidadeResponse(
      tipoEntidade.getId(),
      tipoEntidade.getNome(),
      tipoEntidade.getCodigoSeed(),
      tipoEntidade.isTipoPadrao(),
      tipoEntidade.isAtivo());
  }
}
