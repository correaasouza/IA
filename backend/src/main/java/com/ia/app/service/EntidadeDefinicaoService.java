package com.ia.app.service;

import com.ia.app.domain.EntidadeDefinicao;
import com.ia.app.dto.EntidadeDefinicaoRequest;
import com.ia.app.repository.EntidadeDefinicaoRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class EntidadeDefinicaoService {

  private final EntidadeDefinicaoRepository repository;

  public EntidadeDefinicaoService(EntidadeDefinicaoRepository repository) {
    this.repository = repository;
  }

  public Page<EntidadeDefinicao> list(Pageable pageable) {
    Long tenantId = requireTenant();
    return repository.findAllByTenantId(tenantId, pageable);
  }

  public EntidadeDefinicao create(EntidadeDefinicaoRequest request) {
    Long tenantId = requireTenant();
    EntidadeDefinicao entity = new EntidadeDefinicao();
    entity.setTenantId(tenantId);
    entity.setCodigo(request.codigo());
    entity.setNome(request.nome());
    entity.setAtivo(request.ativo());
    entity.setRoleRequired(request.roleRequired());
    return repository.save(entity);
  }

  public void seedDefaults(Long tenantId) {
    seedIfMissing(tenantId, "CLIENTE", "Cliente");
    seedIfMissing(tenantId, "FUNCIONARIO", "Funcionário");
    seedIfMissing(tenantId, "FORNECEDOR", "Fornecedor");
  }

  private void seedIfMissing(Long tenantId, String codigo, String nome) {
    if (repository.findByTenantIdAndCodigo(tenantId, codigo).isPresent()) {
      return;
    }
    EntidadeDefinicao entity = new EntidadeDefinicao();
    entity.setTenantId(tenantId);
    entity.setCodigo(codigo);
    entity.setNome(nome);
    entity.setAtivo(true);
    repository.save(entity);
  }

  public EntidadeDefinicao update(Long id, EntidadeDefinicaoRequest request) {
    Long tenantId = requireTenant();
    EntidadeDefinicao entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_definicao_not_found"));
    entity.setCodigo(request.codigo());
    entity.setNome(request.nome());
    entity.setAtivo(request.ativo());
    entity.setRoleRequired(request.roleRequired());
    return repository.save(entity);
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
