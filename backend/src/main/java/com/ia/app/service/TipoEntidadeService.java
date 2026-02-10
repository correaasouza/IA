package com.ia.app.service;

import com.ia.app.domain.TipoEntidade;
import com.ia.app.dto.TipoEntidadeRequest;
import com.ia.app.repository.TipoEntidadeRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TipoEntidadeService {

  private final TipoEntidadeRepository repository;

  public TipoEntidadeService(TipoEntidadeRepository repository) {
    this.repository = repository;
  }

  public Page<TipoEntidade> list(Pageable pageable) {
    Long tenantId = requireTenant();
    return repository.findAllByTenantId(tenantId, pageable);
  }

  public TipoEntidade create(TipoEntidadeRequest request) {
    Long tenantId = requireTenant();
    TipoEntidade entity = new TipoEntidade();
    entity.setTenantId(tenantId);
    entity.setCodigo(normalizeCodigo(request.codigo()));
    entity.setNome(request.nome());
    entity.setAtivo(request.ativo());
    entity.setVersao(1);
    return repository.save(entity);
  }

  public void seedDefaults(Long tenantId) {
    seedIfMissing(tenantId, "CLIENTE", "Cliente");
    seedIfMissing(tenantId, "FORNECEDOR", "Fornecedor");
    seedIfMissing(tenantId, "FUNCIONARIO", "Funcionário");
  }

  private void seedIfMissing(Long tenantId, String codigo, String nome) {
    if (repository.findByTenantIdAndCodigo(tenantId, codigo).isPresent()) {
      return;
    }
    TipoEntidade entity = new TipoEntidade();
    entity.setTenantId(tenantId);
    entity.setCodigo(codigo);
    entity.setNome(nome);
    entity.setAtivo(true);
    entity.setVersao(1);
    repository.save(entity);
  }

  public TipoEntidade update(Long id, TipoEntidadeRequest request) {
    Long tenantId = requireTenant();
    TipoEntidade entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("tipo_entidade_not_found"));
    entity.setCodigo(normalizeCodigo(request.codigo()));
    entity.setNome(request.nome());
    entity.setAtivo(request.ativo());
    entity.setVersao(entity.getVersao() + 1);
    return repository.save(entity);
  }

  public Instant maxUpdatedAt() {
    Long tenantId = requireTenant();
    return repository.findMaxUpdatedAt(tenantId);
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private String normalizeCodigo(String codigo) {
    if (codigo == null) return null;
    return codigo.trim().toUpperCase();
  }
}
