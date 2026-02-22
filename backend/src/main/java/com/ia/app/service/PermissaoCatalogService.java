package com.ia.app.service;

import com.ia.app.domain.PermissaoCatalogo;
import com.ia.app.dto.PermissaoCatalogResponse;
import com.ia.app.dto.PermissaoRequest;
import com.ia.app.repository.PermissaoCatalogoRepository;
import com.ia.app.tenant.TenantContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PermissaoCatalogService {

  private final PermissaoCatalogoRepository repository;
  private final AuditService auditService;

  public PermissaoCatalogService(PermissaoCatalogoRepository repository, AuditService auditService) {
    this.repository = repository;
    this.auditService = auditService;
  }

  public List<PermissaoCatalogResponse> listAll() {
    Long tenantId = requireTenant();
    return repository.findAllByTenantIdOrderByCodigo(tenantId)
      .stream().map(p -> new PermissaoCatalogResponse(p.getId(), p.getCodigo(), p.getLabel()))
      .toList();
  }

  public boolean isValid(String codigo) {
    if (codigo == null) return false;
    Long tenantId = requireTenant();
    return repository.findByTenantIdAndCodigo(tenantId, codigo)
      .map(PermissaoCatalogo::isAtivo)
      .orElse(false);
  }

  @Transactional
  public PermissaoCatalogo create(PermissaoRequest request) {
    Long tenantId = requireTenant();
    repository.findByTenantIdAndCodigo(tenantId, request.codigo()).ifPresent(p -> {
      throw new IllegalStateException("permissao_codigo_duplicado");
    });
    PermissaoCatalogo p = new PermissaoCatalogo();
    p.setTenantId(tenantId);
    p.setCodigo(request.codigo());
    p.setLabel(request.label());
    p.setAtivo(request.ativo());
    PermissaoCatalogo saved = repository.save(p);
    auditService.log(tenantId, "PERMISSAO_CRIADA", "permissao", String.valueOf(saved.getId()), "codigo=" + saved.getCodigo());
    return saved;
  }

  @Transactional
  public PermissaoCatalogo update(Long id, PermissaoRequest request) {
    Long tenantId = requireTenant();
    PermissaoCatalogo p = repository.findById(id).orElseThrow();
    if (!p.getTenantId().equals(tenantId)) {
      throw new IllegalStateException("permissao_forbidden");
    }
    if (!p.getCodigo().equalsIgnoreCase(request.codigo())) {
      repository.findByTenantIdAndCodigo(tenantId, request.codigo()).ifPresent(existing -> {
        throw new IllegalStateException("permissao_codigo_duplicado");
      });
    }
    p.setCodigo(request.codigo());
    p.setLabel(request.label());
    p.setAtivo(request.ativo());
    PermissaoCatalogo saved = repository.save(p);
    auditService.log(tenantId, "PERMISSAO_ATUALIZADA", "permissao", String.valueOf(saved.getId()), "codigo=" + saved.getCodigo());
    return saved;
  }

  public void seedDefaults(Long tenantId) {
    seed(tenantId, "CONFIG_EDITOR", "Configurar colunas e formulÃƒÂ¡rios");
    seed(tenantId, "USUARIO_MANAGE", "Gerenciar usuÃƒÂ¡rios");
    seed(tenantId, "PAPEL_MANAGE", "Gerenciar papÃƒÂ©is");
    seed(tenantId, "RELATORIO_VIEW", "Visualizar relatÃƒÂ³rios");
    seed(tenantId, "ENTIDADE_EDIT", "Editar entidades");
    seed(tenantId, "MOVIMENTO_ESTOQUE_OPERAR", "Operar movimento de estoque");
    seed(tenantId, "MOVIMENTO_ITEM_CONFIGURAR", "Configurar tipos de itens de movimento");
    seed(tenantId, "MOVIMENTO_ESTOQUE_ITEM_OPERAR", "Operar itens no movimento de estoque");
    seed(tenantId, "MOVIMENTO_ESTOQUE_DESFAZER", "Desfazer movimentacao de estoque do item");
    seed(tenantId, "CATALOG_PRICES_VIEW", "Visualizar precos de catalogo");
    seed(tenantId, "CATALOG_PRICES_MANAGE", "Gerenciar precos de catalogo");
    seed(tenantId, "WORKFLOW_CONFIGURAR", "Configurar workflows");
    seed(tenantId, "WORKFLOW_TRANSICIONAR", "Executar transicoes de workflow");
  }

  private void seed(Long tenantId, String codigo, String label) {
    repository.findByTenantIdAndCodigo(tenantId, codigo).ifPresentOrElse(p -> {}, () -> {
      PermissaoCatalogo perm = new PermissaoCatalogo();
      perm.setTenantId(tenantId);
      perm.setCodigo(codigo);
      perm.setLabel(label);
      perm.setAtivo(true);
      repository.save(perm);
    });
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) throw new IllegalStateException("tenant_required");
    return tenantId;
  }
}



