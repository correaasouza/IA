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
    validateCpfCnpj(request.cpfCnpj());
    definicaoRepository.findByIdAndTenantId(request.entidadeDefinicaoId(), tenantId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_definicao_not_found"));

    EntidadeRegistro entity = new EntidadeRegistro();
    entity.setTenantId(tenantId);
    entity.setEntidadeDefinicaoId(request.entidadeDefinicaoId());
    entity.setNome(request.nome());
    entity.setApelido(request.apelido());
    entity.setCpfCnpj(request.cpfCnpj());
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
    validateCpfCnpj(request.cpfCnpj());
    EntidadeRegistro entity = existing;
    entity.setNome(request.nome());
    entity.setApelido(request.apelido());
    entity.setCpfCnpj(request.cpfCnpj());
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

  private void validateCpfCnpj(String value) {
    if (!CpfCnpjValidator.isValid(value)) {
      throw new IllegalArgumentException("cpf_cnpj_invalido");
    }
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
