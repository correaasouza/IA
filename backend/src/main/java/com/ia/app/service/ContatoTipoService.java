package com.ia.app.service;

import com.ia.app.domain.ContatoTipo;
import com.ia.app.dto.ContatoTipoRequest;
import com.ia.app.repository.ContatoTipoRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ContatoTipoService {

  private final ContatoTipoRepository repository;

  public ContatoTipoService(ContatoTipoRepository repository) {
    this.repository = repository;
  }

  public List<ContatoTipo> list() {
    Long tenantId = requireTenant();
    return repository.findAllByTenantId(tenantId);
  }

  public ContatoTipo create(ContatoTipoRequest request) {
    Long tenantId = requireTenant();
    ContatoTipo entity = new ContatoTipo();
    entity.setTenantId(tenantId);
    entity.setCodigo(request.codigo());
    entity.setNome(request.nome());
    entity.setAtivo(request.ativo());
    entity.setObrigatorio(request.obrigatorio());
    entity.setPrincipalUnico(request.principalUnico());
    entity.setMascara(request.mascara());
    entity.setRegexValidacao(request.regexValidacao());
    return repository.save(entity);
  }

  public void seedDefaults(Long tenantId) {
    seedIfMissing(tenantId, "TELEFONE", "Telefone");
    seedIfMissing(tenantId, "WHATSAPP", "WhatsApp");
    seedIfMissing(tenantId, "EMAIL", "E-mail");
  }

  private void seedIfMissing(Long tenantId, String codigo, String nome) {
    if (repository.findByTenantIdAndCodigo(tenantId, codigo).isPresent()) {
      return;
    }
    ContatoTipo entity = new ContatoTipo();
    entity.setTenantId(tenantId);
    entity.setCodigo(codigo);
    entity.setNome(nome);
    entity.setAtivo(true);
    entity.setObrigatorio(false);
    entity.setPrincipalUnico(true);
    entity.setMascara(null);
    entity.setRegexValidacao(null);
    repository.save(entity);
  }

  public ContatoTipo update(Long id, ContatoTipoRequest request) {
    Long tenantId = requireTenant();
    ContatoTipo entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("contato_tipo_not_found"));
    entity.setCodigo(request.codigo());
    entity.setNome(request.nome());
    entity.setAtivo(request.ativo());
    entity.setObrigatorio(request.obrigatorio());
    entity.setPrincipalUnico(request.principalUnico());
    entity.setMascara(request.mascara());
    entity.setRegexValidacao(request.regexValidacao());
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
