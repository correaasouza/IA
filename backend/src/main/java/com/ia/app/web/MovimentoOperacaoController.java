package com.ia.app.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.ia.app.domain.MovimentoTipo;
import com.ia.app.dto.MovimentoTemplateRequest;
import com.ia.app.service.MovimentoOperacaoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/movimentos")
public class MovimentoOperacaoController {

  private final MovimentoOperacaoService service;

  public MovimentoOperacaoController(MovimentoOperacaoService service) {
    this.service = service;
  }

  @PostMapping("/{tipo}/template")
  @PreAuthorize("@permissaoGuard.hasPermissao('MOVIMENTO_ESTOQUE_OPERAR')")
  public ResponseEntity<Object> buildTemplate(
      @PathVariable String tipo,
      @RequestBody @Valid MovimentoTemplateRequest request) {
    MovimentoTipo movimentoTipo = MovimentoTipo.from(tipo);
    return ResponseEntity.ok(service.buildTemplate(movimentoTipo, request));
  }

  @PostMapping("/{tipo}")
  @PreAuthorize("@permissaoGuard.hasPermissao('MOVIMENTO_ESTOQUE_OPERAR')")
  public ResponseEntity<Object> create(
      @PathVariable String tipo,
      @RequestBody JsonNode payload) {
    MovimentoTipo movimentoTipo = MovimentoTipo.from(tipo);
    return ResponseEntity.ok(service.create(movimentoTipo, payload));
  }

  @GetMapping("/{tipo}")
  @PreAuthorize("@permissaoGuard.hasPermissao('MOVIMENTO_ESTOQUE_OPERAR')")
  public ResponseEntity<Page<?>> list(
      @PathVariable String tipo,
      @RequestParam(required = false) String nome,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim,
      Pageable pageable) {
    MovimentoTipo movimentoTipo = MovimentoTipo.from(tipo);
    return ResponseEntity.ok(service.list(movimentoTipo, pageable, nome, dataInicio, dataFim));
  }

  @GetMapping("/{tipo}/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('MOVIMENTO_ESTOQUE_OPERAR')")
  public ResponseEntity<Object> getById(
      @PathVariable String tipo,
      @PathVariable Long id) {
    MovimentoTipo movimentoTipo = MovimentoTipo.from(tipo);
    return ResponseEntity.ok(service.get(movimentoTipo, id));
  }

  @PutMapping("/{tipo}/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('MOVIMENTO_ESTOQUE_OPERAR')")
  public ResponseEntity<Object> update(
      @PathVariable String tipo,
      @PathVariable Long id,
      @RequestBody JsonNode payload) {
    MovimentoTipo movimentoTipo = MovimentoTipo.from(tipo);
    return ResponseEntity.ok(service.update(movimentoTipo, id, payload));
  }

  @DeleteMapping("/{tipo}/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('MOVIMENTO_ESTOQUE_OPERAR')")
  public ResponseEntity<Void> delete(
      @PathVariable String tipo,
      @PathVariable Long id) {
    MovimentoTipo movimentoTipo = MovimentoTipo.from(tipo);
    service.delete(movimentoTipo, id);
    return ResponseEntity.noContent().build();
  }
}
