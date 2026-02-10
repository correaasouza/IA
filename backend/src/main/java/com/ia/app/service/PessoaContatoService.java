package com.ia.app.service;

import com.ia.app.domain.PessoaContato;
import com.ia.app.dto.PessoaContatoRequest;
import com.ia.app.repository.PessoaContatoRepository;
import com.ia.app.repository.PessoaRepository;
import com.ia.app.repository.ContatoTipoRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PessoaContatoService {

  private final PessoaContatoRepository repository;
  private final PessoaRepository pessoaRepository;
  private final ContatoTipoRepository contatoTipoRepository;

  public PessoaContatoService(PessoaContatoRepository repository,
      PessoaRepository pessoaRepository,
      ContatoTipoRepository contatoTipoRepository) {
    this.repository = repository;
    this.pessoaRepository = pessoaRepository;
    this.contatoTipoRepository = contatoTipoRepository;
  }

  public List<PessoaContato> list(Long pessoaId) {
    Long tenantId = requireTenant();
    return repository.findAllByTenantIdAndPessoaId(tenantId, pessoaId);
  }

  public PessoaContato create(Long pessoaId, PessoaContatoRequest request) {
    Long tenantId = requireTenant();
    pessoaRepository.findByIdAndTenantId(pessoaId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("pessoa_not_found"));
    validateTipo(tenantId, request.tipo());
    validateValor(tenantId, request.tipo(), request.valor());
    PessoaContato entity = new PessoaContato();
    entity.setTenantId(tenantId);
    entity.setPessoaId(pessoaId);
    entity.setTipo(request.tipo());
    entity.setValor(request.valor());
    entity.setPrincipal(request.principal());
    PessoaContato saved = repository.save(entity);
    ensureSinglePrincipal(saved);
    return saved;
  }

  public PessoaContato update(Long id, PessoaContatoRequest request) {
    Long tenantId = requireTenant();
    PessoaContato entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("pessoa_contato_not_found"));
    validateTipo(tenantId, request.tipo());
    validateValor(tenantId, request.tipo(), request.valor());
    entity.setTipo(request.tipo());
    entity.setValor(request.valor());
    entity.setPrincipal(request.principal());
    PessoaContato saved = repository.save(entity);
    ensureSinglePrincipal(saved);
    return saved;
  }

  public void delete(Long id) {
    Long tenantId = requireTenant();
    PessoaContato entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("pessoa_contato_not_found"));
    repository.delete(entity);
  }

  private void ensureSinglePrincipal(PessoaContato saved) {
    if (!saved.isPrincipal()) return;
    Long tenantId = saved.getTenantId();
    var contatos = repository.findAllByTenantIdAndPessoaId(tenantId, saved.getPessoaId());
    var tipoCfg = contatoTipoRepository.findByTenantIdAndCodigo(tenantId, saved.getTipo()).orElse(null);
    if (tipoCfg != null && !tipoCfg.isPrincipalUnico()) {
      return;
    }
    for (PessoaContato contato : contatos) {
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
