package com.ia.app.service;

import com.ia.app.domain.Entidade;
import com.ia.app.dto.EntidadeRequest;
import com.ia.app.repository.EntidadeRepository;
import com.ia.app.repository.PessoaRepository;
import com.ia.app.repository.TipoEntidadeRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class EntidadeService {

  private final EntidadeRepository repository;
  private final TipoEntidadeRepository tipoRepository;
  private final PessoaRepository pessoaRepository;

  public EntidadeService(EntidadeRepository repository,
      TipoEntidadeRepository tipoRepository,
      PessoaRepository pessoaRepository) {
    this.repository = repository;
    this.tipoRepository = tipoRepository;
    this.pessoaRepository = pessoaRepository;
  }

  public Page<Entidade> list(Long tipoEntidadeId, Pageable pageable) {
    Long tenantId = requireTenant();
    if (tipoEntidadeId != null) {
      return repository.findAllByTenantIdAndTipoEntidadeId(tenantId, tipoEntidadeId, pageable);
    }
    return repository.findAllByTenantId(tenantId, pageable);
  }

  public Entidade get(Long id) {
    Long tenantId = requireTenant();
    return repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_not_found"));
  }

  public Entidade create(EntidadeRequest request) {
    Long tenantId = requireTenant();
    tipoRepository.findByIdAndTenantId(request.tipoEntidadeId(), tenantId)
      .orElseThrow(() -> new EntityNotFoundException("tipo_entidade_not_found"));
    pessoaRepository.findByIdAndTenantId(request.pessoaId(), tenantId)
      .orElseThrow(() -> new EntityNotFoundException("pessoa_not_found"));

    Entidade entity = new Entidade();
    entity.setTenantId(tenantId);
    entity.setTipoEntidadeId(request.tipoEntidadeId());
    entity.setPessoaId(request.pessoaId());
    entity.setAlerta(request.alerta());
    entity.setAtivo(request.ativo());
    entity.setVersao(1);
    return repository.save(entity);
  }

  public Entidade update(Long id, EntidadeRequest request) {
    Long tenantId = requireTenant();
    Entidade entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_not_found"));
    tipoRepository.findByIdAndTenantId(request.tipoEntidadeId(), tenantId)
      .orElseThrow(() -> new EntityNotFoundException("tipo_entidade_not_found"));
    pessoaRepository.findByIdAndTenantId(request.pessoaId(), tenantId)
      .orElseThrow(() -> new EntityNotFoundException("pessoa_not_found"));

    entity.setTipoEntidadeId(request.tipoEntidadeId());
    entity.setPessoaId(request.pessoaId());
    entity.setAlerta(request.alerta());
    entity.setAtivo(request.ativo());
    entity.setVersao(entity.getVersao() + 1);
    return repository.save(entity);
  }

  public void delete(Long id) {
    Long tenantId = requireTenant();
    Entidade entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_not_found"));
    repository.delete(entity);
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
