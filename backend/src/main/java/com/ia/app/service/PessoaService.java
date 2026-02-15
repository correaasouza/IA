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
    String idEstrangeiroNormalizado = normalizeIdEstrangeiro(documento);
    return repository.findByTenantIdAndIdEstrangeiro(tenantId, idEstrangeiroNormalizado)
      .orElseThrow(() -> new EntityNotFoundException("pessoa_not_found"));
  }

  public Pessoa create(PessoaRequest request) {
    Long tenantId = requireTenant();
    String tipoPessoa = resolveTipoPessoa(request);
    DocumentoInfo documento = normalizeDocumento(request, tipoPessoa);
    validateDocumento(tipoPessoa, documento);
    ensureUniqueDocumento(tenantId, null, documento);
    Pessoa entity = new Pessoa();
    entity.setTenantId(tenantId);
    entity.setNome(request.nome());
    entity.setApelido(request.apelido());
    entity.setCpf(documento.cpf);
    entity.setCnpj(documento.cnpj);
    entity.setIdEstrangeiro(documento.idEstrangeiro);
    entity.setTipoPessoa(tipoPessoa);
    entity.setTipoRegistro(toTipoRegistro(tipoPessoa));
    entity.setRegistroFederal(documento.registroFederal);
    entity.setRegistroFederalNormalizado(documento.registroFederalNormalizado);
    entity.setAtivo(request.ativo());
    entity.setVersao(1);
    return repository.save(entity);
  }

  public Pessoa update(Long id, PessoaRequest request) {
    Long tenantId = requireTenant();
    String tipoPessoa = resolveTipoPessoa(request);
    DocumentoInfo documento = normalizeDocumento(request, tipoPessoa);
    validateDocumento(tipoPessoa, documento);
    ensureUniqueDocumento(tenantId, id, documento);
    Pessoa entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("pessoa_not_found"));
    entity.setNome(request.nome());
    entity.setApelido(request.apelido());
    entity.setCpf(documento.cpf);
    entity.setCnpj(documento.cnpj);
    entity.setIdEstrangeiro(documento.idEstrangeiro);
    entity.setTipoPessoa(tipoPessoa);
    entity.setTipoRegistro(toTipoRegistro(tipoPessoa));
    entity.setRegistroFederal(documento.registroFederal);
    entity.setRegistroFederalNormalizado(documento.registroFederalNormalizado);
    entity.setAtivo(request.ativo());
    entity.setVersao(entity.getVersao() + 1);
    return repository.save(entity);
  }

  private void validateDocumento(String tipoPessoa, DocumentoInfo documento) {
    if ("FISICA".equals(tipoPessoa)) {
      if (isBlank(documento.cpf)) {
        throw new IllegalArgumentException("cpf_required");
      }
      if (!CpfCnpjValidator.isValid(documento.cpf)) {
        throw new IllegalArgumentException("cpf_invalido");
      }
      return;
    }
    if ("JURIDICA".equals(tipoPessoa)) {
      if (isBlank(documento.cnpj)) {
        throw new IllegalArgumentException("cnpj_required");
      }
      if (!CpfCnpjValidator.isValid(documento.cnpj)) {
        throw new IllegalArgumentException("cnpj_invalido");
      }
      return;
    }
    if ("ESTRANGEIRA".equals(tipoPessoa)) {
      if (isBlank(documento.idEstrangeiro)) {
        throw new IllegalArgumentException("id_estrangeiro_required");
      }
      return;
    }
    throw new IllegalArgumentException("tipo_pessoa_invalido");
  }

  private void ensureUniqueDocumento(Long tenantId, Long currentId, DocumentoInfo documento) {
    if (!isBlank(documento.cpf)) {
      repository.findByTenantIdAndCpf(tenantId, documento.cpf)
        .filter(p -> currentId == null || !p.getId().equals(currentId))
        .ifPresent(p -> { throw new IllegalArgumentException("cpf_duplicado"); });
    }
    if (!isBlank(documento.cnpj)) {
      repository.findByTenantIdAndCnpj(tenantId, documento.cnpj)
        .filter(p -> currentId == null || !p.getId().equals(currentId))
        .ifPresent(p -> { throw new IllegalArgumentException("cnpj_duplicado"); });
    }
    if (!isBlank(documento.idEstrangeiro)) {
      repository.findByTenantIdAndIdEstrangeiro(tenantId, documento.idEstrangeiro)
        .filter(p -> currentId == null || !p.getId().equals(currentId))
        .ifPresent(p -> { throw new IllegalArgumentException("id_estrangeiro_duplicado"); });
    }
  }

  private DocumentoInfo normalizeDocumento(PessoaRequest request, String tipoPessoa) {
    String cpf = normalize(request.cpf());
    String cnpj = normalize(request.cnpj());
    String idEstrangeiro = normalizeIdEstrangeiro(request.idEstrangeiro());
    String registroFederal;
    String registroFederalNormalizado;
    if ("FISICA".equals(tipoPessoa)) {
      cnpj = null;
      idEstrangeiro = null;
      registroFederal = cpf;
      registroFederalNormalizado = cpf;
    } else if ("JURIDICA".equals(tipoPessoa)) {
      cpf = null;
      idEstrangeiro = null;
      registroFederal = cnpj;
      registroFederalNormalizado = cnpj;
    } else if ("ESTRANGEIRA".equals(tipoPessoa)) {
      cpf = null;
      cnpj = null;
      registroFederal = idEstrangeiro;
      registroFederalNormalizado = idEstrangeiro;
    } else {
      registroFederal = null;
      registroFederalNormalizado = null;
    }
    return new DocumentoInfo(cpf, cnpj, idEstrangeiro, registroFederal, registroFederalNormalizado);
  }

  private String resolveTipoPessoa(PessoaRequest request) {
    if (request.tipoPessoa() != null) {
      String tipo = normalizeTipoPessoa(request.tipoPessoa());
      if (tipo == null) {
        throw new IllegalArgumentException("tipo_pessoa_invalido");
      }
      return tipo;
    }
    if (!isBlank(request.cnpj())) return "JURIDICA";
    if (!isBlank(request.idEstrangeiro())) return "ESTRANGEIRA";
    return "FISICA";
  }

  private String normalizeTipoPessoa(String value) {
    if (value == null) return null;
    String upper = value.trim().toUpperCase();
    if (upper.equals("FISICA") || upper.equals("JURIDICA") || upper.equals("ESTRANGEIRA")) {
      return upper;
    }
    return null;
  }

  private String trimToNull(String value) {
    if (value == null) return null;
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private String normalizeIdEstrangeiro(String value) {
    String trimmed = trimToNull(value);
    if (trimmed == null) return null;
    return trimmed.toUpperCase();
  }

  private String normalize(String value) {
    if (value == null) return null;
    return value.replaceAll("\\D", "");
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String toTipoRegistro(String tipoPessoa) {
    return switch (tipoPessoa) {
      case "FISICA" -> "CPF";
      case "JURIDICA" -> "CNPJ";
      case "ESTRANGEIRA" -> "ID_ESTRANGEIRO";
      default -> throw new IllegalArgumentException("tipo_pessoa_invalido");
    };
  }

  private record DocumentoInfo(
      String cpf,
      String cnpj,
      String idEstrangeiro,
      String registroFederal,
      String registroFederalNormalizado) {}

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
