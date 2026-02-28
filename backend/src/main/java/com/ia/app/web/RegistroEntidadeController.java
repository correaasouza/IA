package com.ia.app.web;

import com.ia.app.dto.RegistroEntidadeEmpresaContextoResponse;
import com.ia.app.dto.RegistroEntidadeRequest;
import com.ia.app.dto.RegistroEntidadeResponse;
import com.ia.app.dto.EntidadeDocumentacaoRequest;
import com.ia.app.dto.EntidadeDocumentacaoResponse;
import com.ia.app.dto.EntidadeEnderecoRequest;
import com.ia.app.dto.EntidadeEnderecoResponse;
import com.ia.app.dto.EntidadeContatoRequest;
import com.ia.app.dto.EntidadeContatoResponse;
import com.ia.app.dto.EntidadeContatoFormaRequest;
import com.ia.app.dto.EntidadeContatoFormaResponse;
import com.ia.app.dto.EntidadeFamiliarRequest;
import com.ia.app.dto.EntidadeFamiliarResponse;
import com.ia.app.dto.EntidadeInfoComercialRequest;
import com.ia.app.dto.EntidadeInfoComercialResponse;
import com.ia.app.dto.EntidadeDadosFiscaisRequest;
import com.ia.app.dto.EntidadeDadosFiscaisResponse;
import com.ia.app.dto.EntidadeContratoRhRequest;
import com.ia.app.dto.EntidadeContratoRhResponse;
import com.ia.app.dto.EntidadeInfoRhRequest;
import com.ia.app.dto.EntidadeInfoRhResponse;
import com.ia.app.dto.EntidadeReferenciaRequest;
import com.ia.app.dto.EntidadeReferenciaResponse;
import com.ia.app.dto.EntidadeQualificacaoItemRequest;
import com.ia.app.dto.EntidadeQualificacaoItemResponse;
import com.ia.app.dto.EntidadeRhOptionsResponse;
import com.ia.app.service.EntidadeSubresourceService;
import com.ia.app.service.EntidadeBusinessRhService;
import com.ia.app.service.RegistroEntidadeContextoService;
import com.ia.app.service.RegistroEntidadeService;
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
@RequestMapping("/api/tipos-entidade/{tipoEntidadeId}/entidades")
public class RegistroEntidadeController {

  private final RegistroEntidadeService service;
  private final RegistroEntidadeContextoService contextoService;
  private final EntidadeSubresourceService subresourceService;
  private final EntidadeBusinessRhService businessRhService;

  public RegistroEntidadeController(
      RegistroEntidadeService service,
      RegistroEntidadeContextoService contextoService,
      EntidadeSubresourceService subresourceService,
      EntidadeBusinessRhService businessRhService) {
    this.service = service;
    this.contextoService = contextoService;
    this.subresourceService = subresourceService;
    this.businessRhService = businessRhService;
  }

  @GetMapping("/contexto-empresa")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<RegistroEntidadeEmpresaContextoResponse> contextoEmpresa(
      @PathVariable Long tipoEntidadeId) {
    return ResponseEntity.ok(contextoService.contexto(tipoEntidadeId));
  }

  @GetMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<Page<RegistroEntidadeResponse>> list(
      @PathVariable Long tipoEntidadeId,
      @RequestParam(required = false) Long codigo,
      @RequestParam(required = false) String pessoaNome,
      @RequestParam(required = false) String registroFederal,
      @RequestParam(required = false) Long grupoId,
      @RequestParam(required = false) Boolean ativo,
      Pageable pageable) {
    return ResponseEntity.ok(service.list(tipoEntidadeId, codigo, pessoaNome, registroFederal, grupoId, ativo, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<RegistroEntidadeResponse> get(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id) {
    return ResponseEntity.ok(service.get(tipoEntidadeId, id));
  }

  @PostMapping
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<RegistroEntidadeResponse> create(
      @PathVariable Long tipoEntidadeId,
      @Valid @RequestBody RegistroEntidadeRequest request) {
    return ResponseEntity.ok(service.create(tipoEntidadeId, request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<RegistroEntidadeResponse> update(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @Valid @RequestBody RegistroEntidadeRequest request) {
    return ResponseEntity.ok(service.update(tipoEntidadeId, id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<Void> delete(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id) {
    service.delete(tipoEntidadeId, id);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/documentacao")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeDocumentacaoResponse> getDocumentacao(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id) {
    return ResponseEntity.ok(subresourceService.getDocumentacao(tipoEntidadeId, id));
  }

  @PutMapping("/{id}/documentacao")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeDocumentacaoResponse> upsertDocumentacao(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @RequestBody EntidadeDocumentacaoRequest request) {
    return ResponseEntity.ok(subresourceService.upsertDocumentacao(tipoEntidadeId, id, request));
  }

  @GetMapping("/{id}/enderecos")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<List<EntidadeEnderecoResponse>> listEnderecos(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id) {
    return ResponseEntity.ok(subresourceService.listEnderecos(tipoEntidadeId, id));
  }

  @PostMapping("/{id}/enderecos")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeEnderecoResponse> createEndereco(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @RequestBody EntidadeEnderecoRequest request) {
    return ResponseEntity.ok(subresourceService.createEndereco(tipoEntidadeId, id, request));
  }

  @PutMapping("/{id}/enderecos/{enderecoId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeEnderecoResponse> updateEndereco(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @PathVariable Long enderecoId,
      @RequestBody EntidadeEnderecoRequest request) {
    return ResponseEntity.ok(subresourceService.updateEndereco(tipoEntidadeId, id, enderecoId, request));
  }

  @DeleteMapping("/{id}/enderecos/{enderecoId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<Void> deleteEndereco(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @PathVariable Long enderecoId) {
    subresourceService.deleteEndereco(tipoEntidadeId, id, enderecoId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/contatos")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<List<EntidadeContatoResponse>> listContatos(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id) {
    return ResponseEntity.ok(subresourceService.listContatos(tipoEntidadeId, id));
  }

  @PostMapping("/{id}/contatos")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeContatoResponse> createContato(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @RequestBody EntidadeContatoRequest request) {
    return ResponseEntity.ok(subresourceService.createContato(tipoEntidadeId, id, request));
  }

  @PutMapping("/{id}/contatos/{contatoId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeContatoResponse> updateContato(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @PathVariable Long contatoId,
      @RequestBody EntidadeContatoRequest request) {
    return ResponseEntity.ok(subresourceService.updateContato(tipoEntidadeId, id, contatoId, request));
  }

  @DeleteMapping("/{id}/contatos/{contatoId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<Void> deleteContato(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @PathVariable Long contatoId) {
    subresourceService.deleteContato(tipoEntidadeId, id, contatoId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/contatos/{contatoId}/formas")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<List<EntidadeContatoFormaResponse>> listContatoFormas(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @PathVariable Long contatoId) {
    return ResponseEntity.ok(subresourceService.listContatoFormas(tipoEntidadeId, id, contatoId));
  }

  @PostMapping("/{id}/contatos/{contatoId}/formas")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeContatoFormaResponse> createContatoForma(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @PathVariable Long contatoId,
      @RequestBody EntidadeContatoFormaRequest request) {
    return ResponseEntity.ok(subresourceService.createContatoForma(tipoEntidadeId, id, contatoId, request));
  }

  @PutMapping("/{id}/contatos/{contatoId}/formas/{formaId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeContatoFormaResponse> updateContatoForma(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @PathVariable Long contatoId,
      @PathVariable Long formaId,
      @RequestBody EntidadeContatoFormaRequest request) {
    return ResponseEntity.ok(subresourceService.updateContatoForma(tipoEntidadeId, id, contatoId, formaId, request));
  }

  @DeleteMapping("/{id}/contatos/{contatoId}/formas/{formaId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<Void> deleteContatoForma(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @PathVariable Long contatoId,
      @PathVariable Long formaId) {
    subresourceService.deleteContatoForma(tipoEntidadeId, id, contatoId, formaId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/familiares")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<List<EntidadeFamiliarResponse>> listFamiliares(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id) {
    return ResponseEntity.ok(subresourceService.listFamiliares(tipoEntidadeId, id));
  }

  @PostMapping("/{id}/familiares")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeFamiliarResponse> createFamiliar(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @RequestBody EntidadeFamiliarRequest request) {
    return ResponseEntity.ok(subresourceService.createFamiliar(tipoEntidadeId, id, request));
  }

  @PutMapping("/{id}/familiares/{familiarId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeFamiliarResponse> updateFamiliar(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @PathVariable Long familiarId,
      @RequestBody EntidadeFamiliarRequest request) {
    return ResponseEntity.ok(subresourceService.updateFamiliar(tipoEntidadeId, id, familiarId, request));
  }

  @DeleteMapping("/{id}/familiares/{familiarId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<Void> deleteFamiliar(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @PathVariable Long familiarId) {
    subresourceService.deleteFamiliar(tipoEntidadeId, id, familiarId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/comercial")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeInfoComercialResponse> getInfoComercial(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id) {
    return ResponseEntity.ok(businessRhService.getInfoComercial(tipoEntidadeId, id));
  }

  @PutMapping("/{id}/comercial")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeInfoComercialResponse> upsertInfoComercial(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @RequestBody EntidadeInfoComercialRequest request) {
    return ResponseEntity.ok(businessRhService.upsertInfoComercial(tipoEntidadeId, id, request));
  }

  @GetMapping("/{id}/dados-fiscais")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeDadosFiscaisResponse> getDadosFiscais(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id) {
    return ResponseEntity.ok(businessRhService.getDadosFiscais(tipoEntidadeId, id));
  }

  @PutMapping("/{id}/dados-fiscais")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeDadosFiscaisResponse> upsertDadosFiscais(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @RequestBody EntidadeDadosFiscaisRequest request) {
    return ResponseEntity.ok(businessRhService.upsertDadosFiscais(tipoEntidadeId, id, request));
  }

  @GetMapping("/{id}/rh/contrato")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeContratoRhResponse> getContratoRh(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id) {
    return ResponseEntity.ok(businessRhService.getContratoRh(tipoEntidadeId, id));
  }

  @PutMapping("/{id}/rh/contrato")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeContratoRhResponse> upsertContratoRh(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @RequestBody EntidadeContratoRhRequest request) {
    return ResponseEntity.ok(businessRhService.upsertContratoRh(tipoEntidadeId, id, request));
  }

  @GetMapping("/{id}/rh/info")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeInfoRhResponse> getInfoRh(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id) {
    return ResponseEntity.ok(businessRhService.getInfoRh(tipoEntidadeId, id));
  }

  @PutMapping("/{id}/rh/info")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeInfoRhResponse> upsertInfoRh(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @RequestBody EntidadeInfoRhRequest request) {
    return ResponseEntity.ok(businessRhService.upsertInfoRh(tipoEntidadeId, id, request));
  }

  @GetMapping("/{id}/rh/referencias")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<List<EntidadeReferenciaResponse>> listReferencias(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id) {
    return ResponseEntity.ok(businessRhService.listReferencias(tipoEntidadeId, id));
  }

  @PostMapping("/{id}/rh/referencias")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeReferenciaResponse> createReferencia(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @RequestBody EntidadeReferenciaRequest request) {
    return ResponseEntity.ok(businessRhService.createReferencia(tipoEntidadeId, id, request));
  }

  @PutMapping("/{id}/rh/referencias/{referenciaId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeReferenciaResponse> updateReferencia(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @PathVariable Long referenciaId,
      @RequestBody EntidadeReferenciaRequest request) {
    return ResponseEntity.ok(businessRhService.updateReferencia(tipoEntidadeId, id, referenciaId, request));
  }

  @DeleteMapping("/{id}/rh/referencias/{referenciaId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<Void> deleteReferencia(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @PathVariable Long referenciaId) {
    businessRhService.deleteReferencia(tipoEntidadeId, id, referenciaId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{id}/rh/qualificacoes")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<List<EntidadeQualificacaoItemResponse>> listQualificacoes(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id) {
    return ResponseEntity.ok(businessRhService.listQualificacoes(tipoEntidadeId, id));
  }

  @PostMapping("/{id}/rh/qualificacoes")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeQualificacaoItemResponse> createQualificacao(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @RequestBody EntidadeQualificacaoItemRequest request) {
    return ResponseEntity.ok(businessRhService.createQualificacao(tipoEntidadeId, id, request));
  }

  @PutMapping("/{id}/rh/qualificacoes/{qualificacaoId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeQualificacaoItemResponse> updateQualificacao(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @PathVariable Long qualificacaoId,
      @RequestBody EntidadeQualificacaoItemRequest request) {
    return ResponseEntity.ok(businessRhService.updateQualificacao(tipoEntidadeId, id, qualificacaoId, request));
  }

  @DeleteMapping("/{id}/rh/qualificacoes/{qualificacaoId}")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<Void> deleteQualificacao(
      @PathVariable Long tipoEntidadeId,
      @PathVariable Long id,
      @PathVariable Long qualificacaoId) {
    businessRhService.deleteQualificacao(tipoEntidadeId, id, qualificacaoId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/rh/opcoes")
  @PreAuthorize("@permissaoGuard.hasPermissao('ENTIDADE_EDIT')")
  public ResponseEntity<EntidadeRhOptionsResponse> loadRhOptions(
      @PathVariable Long tipoEntidadeId) {
    return ResponseEntity.ok(businessRhService.loadRhOptions(tipoEntidadeId));
  }
}
