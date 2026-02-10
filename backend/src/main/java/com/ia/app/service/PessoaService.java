package com.ia.app.service;

import com.ia.app.domain.Pessoa;
import com.ia.app.dto.PessoaRequest;
import com.ia.app.repository.PessoaRepository;
import com.ia.app.tenant.TenantContext;
import com.ia.app.util.CpfCnpjValidator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PessoaService {

  private final PessoaRepository repository;

  public PessoaService(PessoaRepository repository) {
    this.repository = repository;
  }

  public Page<Pessoa> list(Pageable pageable) {
    Long tenantId = requireTenant();
    return repository.findAllByTenantId(tenantId, pageable);
  }

  public Pessoa get(Long id) {
    Long tenantId = requireTenant();
    return repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("pessoa_not_found"));
  }

  public Pessoa findByDocumento(String documento) {
    Long tenantId = requireTenant();
    if (isBlank(documento)) {
      throw new EntityNotFoundException("pessoa_not_found");
    }
    String normalized = normalize(documento);
    if (!isBlank(normalized) && normalized.length() == 11) {
      return repository.findByTenantIdAndCpf(tenantId, normalized)
        .orElseThrow(() -> new EntityNotFoundException("pessoa_not_found"));
    }
    if (!isBlank(normalized) && normalized.length() == 14) {
      return repository.findByTenantIdAndCnpj(tenantId, normalized)
        .orElseThrow(() -> new EntityNotFoundException("pessoa_not_found"));
    }
    return repository.findByTenantIdAndIdEstrangeiro(tenantId, documento)
      .orElseThrow(() -> new EntityNotFoundException("pessoa_not_found"));
  }

  public Pessoa create(PessoaRequest request) {
    Long tenantId = requireTenant();
    validateDocumento(request);
    ensureUniqueDocumento(tenantId, null, request);
    Pessoa entity = new Pessoa();
    entity.setTenantId(tenantId);
    entity.setNome(request.nome());
    entity.setApelido(request.apelido());
    entity.setCpf(normalize(request.cpf()));
    entity.setCnpj(normalize(request.cnpj()));
    entity.setIdEstrangeiro(request.idEstrangeiro());
    entity.setAtivo(request.ativo());
    entity.setVersao(1);
    return repository.save(entity);
  }

  public Pessoa update(Long id, PessoaRequest request) {
    Long tenantId = requireTenant();
    validateDocumento(request);
    ensureUniqueDocumento(tenantId, id, request);
    Pessoa entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("pessoa_not_found"));
    entity.setNome(request.nome());
    entity.setApelido(request.apelido());
    entity.setCpf(normalize(request.cpf()));
    entity.setCnpj(normalize(request.cnpj()));
    entity.setIdEstrangeiro(request.idEstrangeiro());
    entity.setAtivo(request.ativo());
    entity.setVersao(entity.getVersao() + 1);
    return repository.save(entity);
  }

  private void validateDocumento(PessoaRequest request) {
    if (isBlank(request.cpf()) && isBlank(request.cnpj()) && isBlank(request.idEstrangeiro())) {
      throw new IllegalArgumentException("documento_required");
    }
    if (!isBlank(request.cpf()) && !CpfCnpjValidator.isValid(request.cpf())) {
      throw new IllegalArgumentException("cpf_invalido");
    }
    if (!isBlank(request.cnpj()) && !CpfCnpjValidator.isValid(request.cnpj())) {
      throw new IllegalArgumentException("cnpj_invalido");
    }
  }

  private void ensureUniqueDocumento(Long tenantId, Long currentId, PessoaRequest request) {
    String cpf = normalize(request.cpf());
    String cnpj = normalize(request.cnpj());
    String idEstrangeiro = request.idEstrangeiro();
    repository.findByTenantIdAndCpf(tenantId, cpf)
      .filter(p -> currentId == null || !p.getId().equals(currentId))
      .ifPresent(p -> { throw new IllegalArgumentException("cpf_duplicado"); });
    repository.findByTenantIdAndCnpj(tenantId, cnpj)
      .filter(p -> currentId == null || !p.getId().equals(currentId))
      .ifPresent(p -> { throw new IllegalArgumentException("cnpj_duplicado"); });
    repository.findByTenantIdAndIdEstrangeiro(tenantId, idEstrangeiro)
      .filter(p -> currentId == null || !p.getId().equals(currentId))
      .ifPresent(p -> { throw new IllegalArgumentException("id_estrangeiro_duplicado"); });
  }

  private String normalize(String value) {
    if (value == null) return null;
    return value.replaceAll("\\D", "");
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
