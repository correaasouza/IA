package com.ia.app.service;

import com.ia.app.domain.Contato;
import com.ia.app.dto.ContatoRequest;
import com.ia.app.repository.ContatoRepository;
import com.ia.app.repository.ContatoTipoRepository;
import com.ia.app.repository.ContatoTipoPorEntidadeRepository;
import com.ia.app.repository.EntidadeRegistroRepository;
import com.ia.app.domain.ContatoTipo;
import com.ia.app.domain.ContatoTipoPorEntidade;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ContatoService {

  private final ContatoRepository repository;
  private final EntidadeRegistroRepository entidadeRepository;
  private final ContatoTipoRepository contatoTipoRepository;
  private final ContatoTipoPorEntidadeRepository contatoTipoPorEntidadeRepository;

  public ContatoService(ContatoRepository repository, EntidadeRegistroRepository entidadeRepository,
      ContatoTipoRepository contatoTipoRepository,
      ContatoTipoPorEntidadeRepository contatoTipoPorEntidadeRepository) {
    this.repository = repository;
    this.entidadeRepository = entidadeRepository;
    this.contatoTipoRepository = contatoTipoRepository;
    this.contatoTipoPorEntidadeRepository = contatoTipoPorEntidadeRepository;
  }

  public List<Contato> list(Long entidadeRegistroId) {
    Long tenantId = requireTenant();
    return repository.findAllByTenantIdAndEntidadeRegistroId(tenantId, entidadeRegistroId);
  }

  public Contato create(ContatoRequest request) {
    Long tenantId = requireTenant();
    entidadeRepository.findByIdAndTenantId(request.entidadeRegistroId(), tenantId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_registro_not_found"));
    validateTipo(tenantId, request.tipo());
    validateValor(tenantId, request.tipo(), request.valor());
    Contato entity = new Contato();
    entity.setTenantId(tenantId);
    entity.setEntidadeRegistroId(request.entidadeRegistroId());
    entity.setTipo(request.tipo());
    entity.setValor(request.valor());
    entity.setPrincipal(request.principal());
    Contato saved = repository.save(entity);
    ensureSinglePrincipal(saved);
    return saved;
  }

  public Contato update(Long id, ContatoRequest request) {
    Long tenantId = requireTenant();
    Contato entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("contato_not_found"));
    validateTipo(tenantId, request.tipo());
    validateValor(tenantId, request.tipo(), request.valor());
    entity.setTipo(request.tipo());
    entity.setValor(request.valor());
    entity.setPrincipal(request.principal());
    Contato saved = repository.save(entity);
    ensureSinglePrincipal(saved);
    return saved;
  }

  public void delete(Long id) {
    Long tenantId = requireTenant();
    Contato entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("contato_not_found"));
    repository.delete(entity);
  }

  public void deleteByEntidadeRegistro(Long entidadeRegistroId) {
    Long tenantId = requireTenant();
    repository.deleteAllByTenantIdAndEntidadeRegistroId(tenantId, entidadeRegistroId);
  }

  public void validateObrigatorios(Long entidadeRegistroId) {
    Long tenantId = requireTenant();
    var entidade = entidadeRepository.findByIdAndTenantId(entidadeRegistroId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_registro_not_found"));

    var obrigatorios = contatoTipoPorEntidadeRepository
      .findAllByTenantIdAndEntidadeDefinicaoId(tenantId, entidade.getEntidadeDefinicaoId())
      .stream()
      .filter(ContatoTipoPorEntidade::isObrigatorio)
      .toList();
    if (obrigatorios.isEmpty()) return;

    var existentes = repository.findAllByTenantIdAndEntidadeRegistroId(tenantId, entidadeRegistroId);
    for (ContatoTipoPorEntidade tipo : obrigatorios) {
      var contatoTipo = contatoTipoRepository.findByIdAndTenantId(tipo.getContatoTipoId(), tenantId)
        .orElse(null);
      if (contatoTipo == null) continue;
      boolean ok = existentes.stream().anyMatch(c -> c.getTipo().equalsIgnoreCase(contatoTipo.getCodigo()));
      if (!ok) {
        throw new IllegalArgumentException("contato_obrigatorio_" + contatoTipo.getCodigo());
      }
    }
  }

  private void ensureSinglePrincipal(Contato saved) {
    if (!saved.isPrincipal()) return;
    Long tenantId = saved.getTenantId();
    var contatos = repository.findAllByTenantIdAndEntidadeRegistroId(tenantId, saved.getEntidadeRegistroId());
    var tipoCfg = contatoTipoRepository.findByTenantIdAndCodigo(tenantId, saved.getTipo()).orElse(null);
    if (tipoCfg != null && !tipoCfg.isPrincipalUnico()) {
      return;
    }
    var entidade = entidadeRepository.findByIdAndTenantId(saved.getEntidadeRegistroId(), tenantId).orElse(null);
    if (entidade != null) {
      var rel = contatoTipoPorEntidadeRepository
        .findAllByTenantIdAndEntidadeDefinicaoId(tenantId, entidade.getEntidadeDefinicaoId())
        .stream()
        .filter(r -> r.getContatoTipoId().equals(tipoCfg != null ? tipoCfg.getId() : -1L))
        .findFirst().orElse(null);
      if (rel != null && !rel.isPrincipalUnico()) {
        return;
      }
    }
    for (Contato contato : contatos) {
      if (!contato.getId().equals(saved.getId()) && contato.getTipo().equalsIgnoreCase(saved.getTipo())) {
        contato.setPrincipal(false);
        repository.save(contato);
      }
    }
  }

  private void validateTipo(Long tenantId, String tipo) {
    var tipoCfg = contatoTipoRepository.findByTenantIdAndCodigo(tenantId, tipo)
      .orElseThrow(() -> new IllegalArgumentException("contato_tipo_invalido"));
    if (!tipoCfg.isAtivo()) {
      throw new IllegalArgumentException("contato_tipo_inativo");
    }
  }

  private void validateValor(Long tenantId, String tipo, String valor) {
    var tipoCfg = contatoTipoRepository.findByTenantIdAndCodigo(tenantId, tipo)
      .orElseThrow(() -> new IllegalArgumentException("contato_tipo_invalido"));
    String regex = tipoCfg.getRegexValidacao();
    if (regex != null && !regex.isBlank()) {
      if (valor == null || !valor.matches(regex)) {
        throw new IllegalArgumentException("contato_valor_invalido");
      }
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
