package com.ia.app.service;

import com.ia.app.domain.Papel;
import com.ia.app.dto.PapelRequest;
import com.ia.app.repository.PapelRepository;
import com.ia.app.tenant.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PapelService {

  private final PapelRepository repository;
  private final AuditService auditService;

  public PapelService(PapelRepository repository, AuditService auditService) {
    this.repository = repository;
    this.auditService = auditService;
  }

  public List<Papel> list() {
    return repository.findAllByTenantIdOrderByNome(requireTenant());
  }

  public Papel get(Long id) {
    Long tenantId = requireTenant();
    Papel papel = repository.findById(id).orElseThrow();
    if (!papel.getTenantId().equals(tenantId)) {
      throw new IllegalStateException("papel_forbidden");
    }
    return papel;
  }

  @Transactional
  public Papel create(PapelRequest request) {
    Long tenantId = requireTenant();
    repository.findByTenantIdAndNome(tenantId, request.nome()).ifPresent(p -> {
      throw new IllegalStateException("papel_nome_duplicado");
    });
    Papel papel = new Papel();
    papel.setTenantId(tenantId);
    papel.setNome(request.nome());
    papel.setDescricao(request.descricao());
    papel.setAtivo(request.ativo());
    Papel saved = repository.save(papel);
    auditService.log(tenantId, "PAPEL_CRIADO", "papel", String.valueOf(saved.getId()), "nome=" + saved.getNome());
    return saved;
  }

  @Transactional
  public Papel update(Long id, PapelRequest request) {
    Long tenantId = requireTenant();
    Papel papel = repository.findById(id).orElseThrow();
    if (!papel.getTenantId().equals(tenantId)) {
      throw new IllegalStateException("papel_forbidden");
    }
    if (!papel.getNome().equalsIgnoreCase(request.nome())) {
      repository.findByTenantIdAndNome(tenantId, request.nome()).ifPresent(p -> {
        throw new IllegalStateException("papel_nome_duplicado");
      });
    }
    papel.setNome(request.nome());
    papel.setDescricao(request.descricao());
    papel.setAtivo(request.ativo());
    Papel saved = repository.save(papel);
    auditService.log(tenantId, "PAPEL_ATUALIZADO", "papel", String.valueOf(saved.getId()), "nome=" + saved.getNome());
    return saved;
  }

  @Transactional
  public void delete(Long id) {
    Long tenantId = requireTenant();
    Papel papel = repository.findById(id).orElseThrow();
    if (!papel.getTenantId().equals(tenantId)) {
      throw new IllegalStateException("papel_forbidden");
    }
    repository.delete(papel);
    auditService.log(tenantId, "PAPEL_EXCLUIDO", "papel", String.valueOf(id), "nome=" + papel.getNome());
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) throw new IllegalStateException("tenant_required");
    return tenantId;
  }
}
