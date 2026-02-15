package com.ia.app.service;

import com.ia.app.domain.UsuarioEmpresaPreferencia;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.repository.UsuarioEmpresaPreferenciaRepository;
import com.ia.app.tenant.TenantContext;
import org.springframework.stereotype.Service;

@Service
public class UsuarioEmpresaPreferenciaService {
  private final UsuarioEmpresaPreferenciaRepository repository;
  private final EmpresaRepository empresaRepository;

  public UsuarioEmpresaPreferenciaService(
      UsuarioEmpresaPreferenciaRepository repository,
      EmpresaRepository empresaRepository) {
    this.repository = repository;
    this.empresaRepository = empresaRepository;
  }

  public Long getEmpresaPadraoId(String usuarioId) {
    Long tenantId = requireTenant();
    Long empresaId = repository.findByTenantIdAndUsuarioId(tenantId, usuarioId)
      .map(pref -> {
        Long empresaIdPref = pref.getEmpresaPadraoId();
        if (empresaIdPref == null || !empresaRepository.existsByIdAndTenantId(empresaIdPref, tenantId)) {
          repository.delete(pref);
          return null;
        }
        return empresaIdPref;
      })
      .orElse(null);
    return empresaId;
  }

  public Long setEmpresaPadraoId(String usuarioId, Long empresaId) {
    Long tenantId = requireTenant();
    if (empresaId == null) {
      throw new IllegalArgumentException("empresa_padrao_id_required");
    }
    if (!empresaRepository.existsByIdAndTenantId(empresaId, tenantId)) {
      throw new IllegalArgumentException("empresa_not_found");
    }
    UsuarioEmpresaPreferencia pref = repository.findByTenantIdAndUsuarioId(tenantId, usuarioId)
      .orElseGet(() -> {
        UsuarioEmpresaPreferencia created = new UsuarioEmpresaPreferencia();
        created.setTenantId(tenantId);
        created.setUsuarioId(usuarioId);
        return created;
      });
    pref.setEmpresaPadraoId(empresaId);
    return repository.save(pref).getEmpresaPadraoId();
  }

  public void clearEmpresaPadraoId(String usuarioId) {
    Long tenantId = requireTenant();
    repository.deleteByTenantIdAndUsuarioId(tenantId, usuarioId);
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
