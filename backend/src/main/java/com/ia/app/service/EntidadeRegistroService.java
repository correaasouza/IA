package com.ia.app.service;

import com.ia.app.domain.EntidadeRegistro;
import com.ia.app.dto.EntidadeRegistroRequest;
import com.ia.app.dto.EntidadeRegistroUpdateRequest;
import com.ia.app.repository.EntidadeDefinicaoRepository;
import com.ia.app.repository.EntidadeRegistroRepository;
import com.ia.app.security.RoleGuard;
import com.ia.app.util.CpfCnpjValidator;
import com.ia.app.service.ContatoService;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class EntidadeRegistroService {

  private final EntidadeRegistroRepository repository;
  private final EntidadeDefinicaoRepository definicaoRepository;
  private final RoleGuard roleGuard;
  private final ContatoService contatoService;

  public EntidadeRegistroService(EntidadeRegistroRepository repository,
      EntidadeDefinicaoRepository definicaoRepository,
      RoleGuard roleGuard,
      ContatoService contatoService) {
    this.repository = repository;
    this.definicaoRepository = definicaoRepository;
    this.roleGuard = roleGuard;
    this.contatoService = contatoService;
  }

  public Page<EntidadeRegistro> list(Long entidadeDefinicaoId, String nome, String cpfCnpj, Boolean ativo, Pageable pageable) {
    Long tenantId = requireTenant();
    ensureRole(entidadeDefinicaoId, tenantId);
    if (nome != null && !nome.isBlank()) {
      return repository.findAllByTenantIdAndEntidadeDefinicaoIdAndNomeContainingIgnoreCase(
        tenantId, entidadeDefinicaoId, nome, pageable);
    }
    if (cpfCnpj != null && !cpfCnpj.isBlank()) {
      return repository.findAllByTenantIdAndEntidadeDefinicaoIdAndCpfCnpjContainingIgnoreCase(
        tenantId, entidadeDefinicaoId, cpfCnpj, pageable);
    }
    if (ativo != null) {
      return repository.findAllByTenantIdAndEntidadeDefinicaoIdAndAtivo(
        tenantId, entidadeDefinicaoId, ativo, pageable);
    }
    return repository.findAllByTenantIdAndEntidadeDefinicaoId(tenantId, entidadeDefinicaoId, pageable);
  }

  public EntidadeRegistro create(EntidadeRegistroRequest request) {
    Long tenantId = requireTenant();
    ensureRole(request.entidadeDefinicaoId(), tenantId);
    String tipoPessoa = resolveTipoPessoa(request.tipoPessoa(), request.cpfCnpj());
    String documento = normalizeDocumento(request.cpfCnpj(), tipoPessoa);
    validateDocumento(tipoPessoa, documento);
    definicaoRepository.findByIdAndTenantId(request.entidadeDefinicaoId(), tenantId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_definicao_not_found"));

    EntidadeRegistro entity = new EntidadeRegistro();
    entity.setTenantId(tenantId);
    entity.setEntidadeDefinicaoId(request.entidadeDefinicaoId());
    entity.setNome(request.nome());
    entity.setApelido(request.apelido());
    entity.setCpfCnpj(documento);
    entity.setTipoPessoa(tipoPessoa);
    entity.setAtivo(request.ativo());
    EntidadeRegistro saved = repository.save(entity);
    if (request.ativo()) {
      contatoService.validateObrigatorios(saved.getId());
    }
    return saved;
  }

  public EntidadeRegistro update(Long id, EntidadeRegistroUpdateRequest request) {
    Long tenantId = requireTenant();
    EntidadeRegistro existing = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_registro_not_found"));
    ensureRole(existing.getEntidadeDefinicaoId(), tenantId);
    String tipoPessoa = resolveTipoPessoa(request.tipoPessoa(), request.cpfCnpj());
    String documento = normalizeDocumento(request.cpfCnpj(), tipoPessoa);
    validateDocumento(tipoPessoa, documento);
    EntidadeRegistro entity = existing;
    entity.setNome(request.nome());
    entity.setApelido(request.apelido());
    entity.setCpfCnpj(documento);
    entity.setTipoPessoa(tipoPessoa);
    entity.setAtivo(request.ativo());
    entity.setVersao(entity.getVersao() + 1);
    EntidadeRegistro saved = repository.save(entity);
    if (request.ativo()) {
      contatoService.validateObrigatorios(saved.getId());
    }
    return saved;
  }

  public EntidadeRegistro getById(Long id) {
    Long tenantId = requireTenant();
    EntidadeRegistro entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_registro_not_found"));
    ensureRole(entity.getEntidadeDefinicaoId(), tenantId);
    return entity;
  }

  public void delete(Long id) {
    Long tenantId = requireTenant();
    EntidadeRegistro entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_registro_not_found"));
    ensureRole(entity.getEntidadeDefinicaoId(), tenantId);
    contatoService.deleteByEntidadeRegistro(entity.getId());
    repository.delete(entity);
  }

  private void ensureRole(Long entidadeDefinicaoId, Long tenantId) {
    var definicao = definicaoRepository.findByIdAndTenantId(entidadeDefinicaoId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_definicao_not_found"));
    String required = definicao.getRoleRequired();
    if (required != null && !required.isBlank()) {
      if (!roleGuard.hasRole(required)) {
        throw new IllegalStateException("role_required_" + required);
      }
    }
  }

  private void validateDocumento(String tipoPessoa, String documento) {
    if ("FISICA".equals(tipoPessoa)) {
      if (documento == null || documento.isBlank()) {
        throw new IllegalArgumentException("cpf_required");
      }
      if (!CpfCnpjValidator.isValid(documento)) {
        throw new IllegalArgumentException("cpf_invalido");
      }
      return;
    }
    if ("JURIDICA".equals(tipoPessoa)) {
      if (documento == null || documento.isBlank()) {
        throw new IllegalArgumentException("cnpj_required");
      }
      if (!CpfCnpjValidator.isValid(documento)) {
        throw new IllegalArgumentException("cnpj_invalido");
      }
      return;
    }
    if ("ESTRANGEIRA".equals(tipoPessoa)) {
      if (documento == null || documento.isBlank()) {
        throw new IllegalArgumentException("id_estrangeiro_required");
      }
      return;
    }
    throw new IllegalArgumentException("tipo_pessoa_invalido");
  }

  private String resolveTipoPessoa(String tipoPessoa, String documento) {
    if (tipoPessoa != null) {
      String normalized = normalizeTipoPessoa(tipoPessoa);
      if (normalized == null) {
        throw new IllegalArgumentException("tipo_pessoa_invalido");
      }
      return normalized;
    }
    String digits = documento == null ? "" : documento.replaceAll("\\D", "");
    if (digits.length() == 14) return "JURIDICA";
    if (digits.length() == 11) return "FISICA";
    return "ESTRANGEIRA";
  }

  private String normalizeTipoPessoa(String value) {
    if (value == null) return null;
    String upper = value.trim().toUpperCase();
    if (upper.equals("FISICA") || upper.equals("JURIDICA") || upper.equals("ESTRANGEIRA")) {
      return upper;
    }
    return null;
  }

  private String normalizeDocumento(String value, String tipoPessoa) {
    if (value == null) return null;
    if ("ESTRANGEIRA".equals(tipoPessoa)) {
      return value.trim();
    }
    return value.replaceAll("\\D", "");
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
