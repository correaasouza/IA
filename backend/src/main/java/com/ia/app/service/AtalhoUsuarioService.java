package com.ia.app.service;

import com.ia.app.domain.AtalhoUsuario;
import com.ia.app.dto.AtalhoUsuarioOrdemRequest;
import com.ia.app.dto.AtalhoUsuarioRequest;
import com.ia.app.repository.AtalhoUsuarioRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AtalhoUsuarioService {

  private final AtalhoUsuarioRepository repository;

  public AtalhoUsuarioService(AtalhoUsuarioRepository repository) {
    this.repository = repository;
  }

  public List<AtalhoUsuario> list(String userId) {
    Long tenantId = requireTenant();
    return repository.findAllByTenantIdAndUserIdOrderByOrdemAsc(tenantId, userId);
  }

  public AtalhoUsuario create(String userId, AtalhoUsuarioRequest request) {
    Long tenantId = requireTenant();
    return repository.findByTenantIdAndUserIdAndMenuId(tenantId, userId, request.menuId())
      .orElseGet(() -> {
        AtalhoUsuario entity = new AtalhoUsuario();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setMenuId(request.menuId());
        entity.setIcon(request.icon());
        entity.setOrdem(request.ordem());
        return repository.save(entity);
      });
  }

  public void delete(String userId, Long id) {
    Long tenantId = requireTenant();
    AtalhoUsuario entity = repository.findByIdAndTenantIdAndUserId(id, tenantId, userId)
      .orElseThrow(() -> new EntityNotFoundException("atalho_not_found"));
    repository.delete(entity);
  }

  public void reorder(String userId, List<AtalhoUsuarioOrdemRequest> ordens) {
    Long tenantId = requireTenant();
    for (AtalhoUsuarioOrdemRequest req : ordens) {
      AtalhoUsuario entity = repository.findByIdAndTenantIdAndUserId(req.id(), tenantId, userId)
        .orElseThrow(() -> new EntityNotFoundException("atalho_not_found"));
      entity.setOrdem(req.ordem());
      repository.save(entity);
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
