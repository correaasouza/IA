package com.ia.app.service;

import com.ia.app.domain.TipoEntidadeCampoRegra;
import com.ia.app.dto.TipoEntidadeCampoRegraRequest;
import com.ia.app.repository.TipoEntidadeCampoRegraRepository;
import com.ia.app.repository.TipoEntidadeRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TipoEntidadeCampoRegraService {

  private static final List<CampoFixo> CAMPOS_FIXOS = List.of(
    new CampoFixo("nome", "Nome", true),
    new CampoFixo("apelido", "Apelido", false),
    new CampoFixo("cpf", "CPF", false),
    new CampoFixo("cnpj", "CNPJ", false),
    new CampoFixo("id_estrangeiro", "ID estrangeiro", false)
  );

  private final TipoEntidadeCampoRegraRepository repository;
  private final TipoEntidadeRepository tipoRepository;

  public TipoEntidadeCampoRegraService(
      TipoEntidadeCampoRegraRepository repository,
      TipoEntidadeRepository tipoRepository) {
    this.repository = repository;
    this.tipoRepository = tipoRepository;
  }

  public List<TipoEntidadeCampoRegra> list(Long tipoEntidadeId) {
    Long tenantId = requireTenant();
    tipoRepository.findByIdAndTenantId(tipoEntidadeId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("tipo_entidade_not_found"));
    ensureDefaults(tenantId, tipoEntidadeId);
    return repository.findAllByTenantIdAndTipoEntidadeId(tenantId, tipoEntidadeId);
  }

  public List<TipoEntidadeCampoRegra> saveAll(Long tipoEntidadeId, List<TipoEntidadeCampoRegraRequest> requests) {
    Long tenantId = requireTenant();
    tipoRepository.findByIdAndTenantId(tipoEntidadeId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("tipo_entidade_not_found"));
    ensureDefaults(tenantId, tipoEntidadeId);
    List<TipoEntidadeCampoRegra> updated = new ArrayList<>();
    for (TipoEntidadeCampoRegraRequest request : requests) {
      TipoEntidadeCampoRegra entity = repository
        .findByTenantIdAndTipoEntidadeIdAndCampo(tenantId, tipoEntidadeId, request.campo())
        .orElseGet(() -> {
          TipoEntidadeCampoRegra created = new TipoEntidadeCampoRegra();
          created.setTenantId(tenantId);
          created.setTipoEntidadeId(tipoEntidadeId);
          created.setCampo(request.campo());
          created.setVersao(1);
          return created;
        });
      entity.setHabilitado(request.habilitado());
      entity.setRequerido(request.requerido());
      entity.setVisivel(request.visivel());
      entity.setEditavel(request.editavel());
      entity.setLabel(request.label());
      if (entity.getId() != null) {
        entity.setVersao(entity.getVersao() + 1);
      }
      updated.add(repository.save(entity));
    }
    return updated;
  }

  private void ensureDefaults(Long tenantId, Long tipoEntidadeId) {
    for (CampoFixo campo : CAMPOS_FIXOS) {
      if (repository.findByTenantIdAndTipoEntidadeIdAndCampo(tenantId, tipoEntidadeId, campo.nome).isPresent()) {
        continue;
      }
      TipoEntidadeCampoRegra entity = new TipoEntidadeCampoRegra();
      entity.setTenantId(tenantId);
      entity.setTipoEntidadeId(tipoEntidadeId);
      entity.setCampo(campo.nome);
      entity.setHabilitado(true);
      entity.setRequerido(campo.requerido);
      entity.setVisivel(true);
      entity.setEditavel(true);
      entity.setLabel(campo.label);
      entity.setVersao(1);
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

  private record CampoFixo(String nome, String label, boolean requerido) {}
}
