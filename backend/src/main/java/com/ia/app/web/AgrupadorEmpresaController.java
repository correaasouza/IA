package com.ia.app.web;

import com.ia.app.dto.AgrupadorEmpresaCreateRequest;
import com.ia.app.dto.AgrupadorEmpresaEmpresaRequest;
import com.ia.app.dto.AgrupadorEmpresaRenameRequest;
import com.ia.app.dto.AgrupadorEmpresaResponse;
import com.ia.app.service.AgrupadorEmpresaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracoes/{configType}/{configId}/agrupadores-empresa")
public class AgrupadorEmpresaController {

  private final AgrupadorEmpresaService service;

  public AgrupadorEmpresaController(AgrupadorEmpresaService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("@configuracaoPermissaoGuard.canGerenciarAgrupadores(#configType)")
  public ResponseEntity<List<AgrupadorEmpresaResponse>> listar(
      @PathVariable String configType,
      @PathVariable Long configId) {
    return ResponseEntity.ok(service.listar(configType, configId));
  }

  @GetMapping("/{agrupadorId}")
  @PreAuthorize("@configuracaoPermissaoGuard.canGerenciarAgrupadores(#configType)")
  public ResponseEntity<AgrupadorEmpresaResponse> detalhe(
      @PathVariable String configType,
      @PathVariable Long configId,
      @PathVariable Long agrupadorId) {
    return ResponseEntity.ok(service.detalhe(configType, configId, agrupadorId));
  }

  @PostMapping
  @PreAuthorize("@configuracaoPermissaoGuard.canGerenciarAgrupadores(#configType)")
  public ResponseEntity<AgrupadorEmpresaResponse> criar(
      @PathVariable String configType,
      @PathVariable Long configId,
      @Valid @RequestBody AgrupadorEmpresaCreateRequest request) {
    return ResponseEntity.ok(service.criar(configType, configId, request.nome()));
  }

  @PatchMapping("/{agrupadorId}/nome")
  @PreAuthorize("@configuracaoPermissaoGuard.canGerenciarAgrupadores(#configType)")
  public ResponseEntity<AgrupadorEmpresaResponse> renomear(
      @PathVariable String configType,
      @PathVariable Long configId,
      @PathVariable Long agrupadorId,
      @Valid @RequestBody AgrupadorEmpresaRenameRequest request) {
    return ResponseEntity.ok(service.renomear(configType, configId, agrupadorId, request.nome()));
  }

  @PostMapping("/{agrupadorId}/empresas")
  @PreAuthorize("@configuracaoPermissaoGuard.canGerenciarAgrupadores(#configType)")
  public ResponseEntity<AgrupadorEmpresaResponse> adicionarEmpresa(
      @PathVariable String configType,
      @PathVariable Long configId,
      @PathVariable Long agrupadorId,
      @Valid @RequestBody AgrupadorEmpresaEmpresaRequest request) {
    return ResponseEntity.ok(service.adicionarEmpresa(configType, configId, agrupadorId, request.empresaId()));
  }

  @DeleteMapping("/{agrupadorId}/empresas/{empresaId}")
  @PreAuthorize("@configuracaoPermissaoGuard.canGerenciarAgrupadores(#configType)")
  public ResponseEntity<AgrupadorEmpresaResponse> removerEmpresa(
      @PathVariable String configType,
      @PathVariable Long configId,
      @PathVariable Long agrupadorId,
      @PathVariable Long empresaId) {
    return ResponseEntity.ok(service.removerEmpresa(configType, configId, agrupadorId, empresaId));
  }

  @DeleteMapping("/{agrupadorId}")
  @PreAuthorize("@configuracaoPermissaoGuard.canGerenciarAgrupadores(#configType)")
  public ResponseEntity<Void> removerAgrupador(
      @PathVariable String configType,
      @PathVariable Long configId,
      @PathVariable Long agrupadorId) {
    service.removerAgrupador(configType, configId, agrupadorId);
    return ResponseEntity.noContent().build();
  }
}
