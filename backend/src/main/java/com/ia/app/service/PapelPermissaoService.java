package com.ia.app.service;

import com.ia.app.domain.Papel;
import com.ia.app.domain.PapelPermissao;
import com.ia.app.repository.PapelPermissaoRepository;
import com.ia.app.repository.PapelRepository;
import com.ia.app.tenant.TenantContext;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PapelPermissaoService {

  private final PapelPermissaoRepository repository;
  private final PapelRepository papelRepository;
  private final PermissaoCatalogService catalogService;
  private final AuditService auditService;

  public PapelPermissaoService(PapelPermissaoRepository repository,
      PapelRepository papelRepository,
      PermissaoCatalogService catalogService,
      AuditService auditService) {
    this.repository = repository;
    this.papelRepository = papelRepository;
    this.catalogService = catalogService;
    this.auditService = auditService;
  }

  public List<String> listPermissoes(Long papelId) {
    Long tenantId = requireTenant();
    ensurePapelTenant(papelId, tenantId);
    return repository.findAllByTenantIdAndPapelId(tenantId, papelId)
      .stream().map(PapelPermissao::getPermissaoCodigo).toList();
  }

  @Transactional
  @CacheEvict(value = "permissoesUsuario", allEntries = true)
  public void setPermissoes(Long papelId, List<String> permissoes) {
    Long tenantId = requireTenant();
    ensurePapelTenant(papelId, tenantId);
    if (permissoes == null) {
      throw new IllegalStateException("permissao_lista_vazia");
    }
    for (String codigo : permissoes) {
      if (!catalogService.isValid(codigo)) {
        throw new IllegalStateException("permissao_invalida_" + codigo);
      }
    }
    repository.deleteAllByTenantIdAndPapelId(tenantId, papelId);
    for (String codigo : permissoes) {
      PapelPermissao pp = new PapelPermissao();
      pp.setTenantId(tenantId);
      pp.setPapelId(papelId);
      pp.setPermissaoCodigo(codigo);
      repository.save(pp);
    }
    auditService.log(tenantId, "PAPEL_PERMISSOES_ATUALIZADAS", "papel", String.valueOf(papelId),
      "permissoes=" + String.join(",", permissoes));
  }

  private void ensurePapelTenant(Long papelId, Long tenantId) {
    Papel papel = papelRepository.findById(papelId).orElseThrow();
    if (!papel.getTenantId().equals(tenantId)) {
      throw new IllegalStateException("papel_forbidden");
    }
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) throw new IllegalStateException("tenant_required");
    return tenantId;
  }
}
