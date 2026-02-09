package com.ia.app.web;

import com.ia.app.dto.PapelPermissaoRequest;
import com.ia.app.dto.PapelRequest;
import com.ia.app.dto.PapelResponse;
import com.ia.app.service.PapelPermissaoService;
import com.ia.app.service.PapelService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/papeis")
public class PapelController {

  private final PapelService papelService;
  private final PapelPermissaoService papelPermissaoService;

  public PapelController(PapelService papelService, PapelPermissaoService papelPermissaoService) {
    this.papelService = papelService;
    this.papelPermissaoService = papelPermissaoService;
  }

  @GetMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('PAPEL_MANAGE')")
  public List<PapelResponse> list() {
    return papelService.list().stream()
      .map(p -> new PapelResponse(p.getId(), p.getNome(), p.getDescricao(), p.isAtivo()))
      .toList();
  }

  @PostMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('PAPEL_MANAGE')")
  public ResponseEntity<PapelResponse> create(@Valid @RequestBody PapelRequest request) {
    var p = papelService.create(request);
    return ResponseEntity.ok(new PapelResponse(p.getId(), p.getNome(), p.getDescricao(), p.isAtivo()));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('PAPEL_MANAGE')")
  public ResponseEntity<PapelResponse> update(@PathVariable Long id, @Valid @RequestBody PapelRequest request) {
    var p = papelService.update(id, request);
    return ResponseEntity.ok(new PapelResponse(p.getId(), p.getNome(), p.getDescricao(), p.isAtivo()));
  }

  @GetMapping("/{id}/permissoes")
  @PreAuthorize("@permissaoGuard.hasPermissao('PAPEL_MANAGE')")
  public List<String> listPermissoes(@PathVariable Long id) {
    return papelPermissaoService.listPermissoes(id);
  }

  @PostMapping("/{id}/permissoes")
  @PreAuthorize("@permissaoGuard.hasPermissao('PAPEL_MANAGE')")
  public ResponseEntity<Void> setPermissoes(@PathVariable Long id, @Valid @RequestBody PapelPermissaoRequest request) {
    papelPermissaoService.setPermissoes(id, request.permissoes());
    return ResponseEntity.noContent().build();
  }
}
