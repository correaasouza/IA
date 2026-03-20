package com.ia.app.service;

import com.ia.app.domain.Pessoa;
import com.ia.app.dto.PessoaVinculoRequest;
import com.ia.app.repository.PessoaRepository;
import com.ia.app.util.CpfCnpjValidator;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PessoaResolveService {

  private final PessoaRepository repository;
  private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();
  private final TransactionTemplate requiresNewTx;

  public PessoaResolveService(
      PessoaRepository repository,
      PlatformTransactionManager txManager) {
    this.repository = repository;
    this.requiresNewTx = new TransactionTemplate(txManager);
    this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Transactional
  public Pessoa resolveOrCreate(Long tenantId, PessoaVinculoRequest request) {
    String nome = normalizeNome(request.nome());
    String apelido = normalizeOptional(request.apelido());
    String tipoRegistro = normalizeTipoRegistro(request.tipoRegistro());
    String registroFederalNormalizado = normalizeRegistroFederal(tipoRegistro, request.registroFederal());
    String tipoPessoa = normalizeTipoPessoa(request.tipoPessoa(), tipoRegistro);
    String genero = normalizeOptionalWithMax(request.genero(), 30, "pessoa_genero_too_long");
    String nacionalidade = normalizeOptionalWithMax(request.nacionalidade(), 120, "pessoa_nacionalidade_too_long");
    String naturalidade = normalizeOptionalWithMax(request.naturalidade(), 120, "pessoa_naturalidade_too_long");
    String estadoCivil = normalizeOptionalWithMax(request.estadoCivil(), 30, "pessoa_estado_civil_too_long");
    LocalDate dataNascimento = parseDate(request.dataNascimento(), "pessoa_data_nascimento_invalid");
    String lockKey = tenantId + "|" + tipoRegistro + "|" + registroFederalNormalizado;

    Object lock = locks.computeIfAbsent(lockKey, k -> new Object());
    synchronized (lock) {
      try {
        Pessoa existing = repository.findByTenantIdAndTipoRegistroAndRegistroFederalNormalizado(
          tenantId, tipoRegistro, registroFederalNormalizado).orElse(null);
        if (existing != null) {
          return existing;
        }

        try {
          Pessoa created = requiresNewTx.execute(status -> repository.save(buildPessoa(
            tenantId,
            nome,
            apelido,
            tipoRegistro,
            registroFederalNormalizado,
            tipoPessoa,
            genero,
            nacionalidade,
            naturalidade,
            estadoCivil,
            dataNascimento)));
          if (created == null) {
            throw new IllegalStateException("pessoa_create_failed");
          }
          return created;
        } catch (DataIntegrityViolationException ex) {
          return repository.findByTenantIdAndTipoRegistroAndRegistroFederalNormalizado(
              tenantId, tipoRegistro, registroFederalNormalizado)
            .orElseThrow(() -> new IllegalArgumentException("pessoa_registro_federal_duplicado"));
        }
      } finally {
        locks.remove(lockKey, lock);
      }
    }
  }

  @Transactional
  public Pessoa updateLinkedPessoa(Long tenantId, Long pessoaId, PessoaVinculoRequest request) {
    Pessoa existing = repository.findByIdAndTenantId(pessoaId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("pessoa_not_found"));

    String nome = normalizeNome(request.nome());
    String apelido = normalizeOptional(request.apelido());
    String tipoRegistro = normalizeTipoRegistro(request.tipoRegistro());
    String registroFederalNormalizado = normalizeRegistroFederal(tipoRegistro, request.registroFederal());
    String tipoPessoa = normalizeTipoPessoa(request.tipoPessoa(), tipoRegistro);
    String genero = normalizeOptionalWithMax(request.genero(), 30, "pessoa_genero_too_long");
    String nacionalidade = normalizeOptionalWithMax(request.nacionalidade(), 120, "pessoa_nacionalidade_too_long");
    String naturalidade = normalizeOptionalWithMax(request.naturalidade(), 120, "pessoa_naturalidade_too_long");
    String estadoCivil = normalizeOptionalWithMax(request.estadoCivil(), 30, "pessoa_estado_civil_too_long");
    LocalDate dataNascimento = parseDate(request.dataNascimento(), "pessoa_data_nascimento_invalid");

    existing.setNome(nome);
    existing.setApelido(apelido);
    existing.setTipoRegistro(tipoRegistro);
    existing.setRegistroFederal(registroFederalNormalizado);
    existing.setRegistroFederalNormalizado(registroFederalNormalizado);
    existing.setTipoPessoa(tipoPessoa);
    existing.setGenero(genero);
    existing.setNacionalidade(nacionalidade);
    existing.setNaturalidade(naturalidade);
    existing.setEstadoCivil(estadoCivil);
    existing.setDataNascimento(dataNascimento);

    existing.setCpf(null);
    existing.setCnpj(null);
    existing.setIdEstrangeiro(null);
    if ("CPF".equals(tipoRegistro)) {
      existing.setCpf(registroFederalNormalizado);
    } else if ("CNPJ".equals(tipoRegistro)) {
      existing.setCnpj(registroFederalNormalizado);
    } else {
      existing.setIdEstrangeiro(registroFederalNormalizado);
    }

    try {
      return repository.save(existing);
    } catch (DataIntegrityViolationException ex) {
      String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage().toLowerCase();
      if (message.contains("ux_pessoa_tenant_tipo_registro_federal_norm")) {
        throw new IllegalArgumentException("pessoa_registro_federal_duplicado");
      }
      throw ex;
    }
  }

  private Pessoa buildPessoa(
      Long tenantId,
      String nome,
      String apelido,
      String tipoRegistro,
      String registroFederalNormalizado,
      String tipoPessoa,
      String genero,
      String nacionalidade,
      String naturalidade,
      String estadoCivil,
      LocalDate dataNascimento) {
    Pessoa entity = new Pessoa();
    entity.setTenantId(tenantId);
    entity.setNome(nome);
    entity.setApelido(apelido);
    entity.setTipoRegistro(tipoRegistro);
    entity.setRegistroFederal(registroFederalNormalizado);
    entity.setRegistroFederalNormalizado(registroFederalNormalizado);
    entity.setTipoPessoa(tipoPessoa);
    entity.setGenero(genero);
    entity.setNacionalidade(nacionalidade);
    entity.setNaturalidade(naturalidade);
    entity.setEstadoCivil(estadoCivil);
    entity.setDataNascimento(dataNascimento);
    entity.setAtivo(true);
    entity.setVersao(1);
    if ("CPF".equals(tipoRegistro)) {
      entity.setCpf(registroFederalNormalizado);
    } else if ("CNPJ".equals(tipoRegistro)) {
      entity.setCnpj(registroFederalNormalizado);
    } else {
      entity.setIdEstrangeiro(registroFederalNormalizado);
    }
    return entity;
  }

  private String normalizeNome(String nome) {
    if (nome == null || nome.isBlank()) {
      throw new IllegalArgumentException("pessoa_nome_required");
    }
    String value = nome.trim();
    if (value.length() > 200) {
      throw new IllegalArgumentException("pessoa_nome_too_long");
    }
    return value;
  }

  private String normalizeOptional(String value) {
    if (value == null) return null;
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private String normalizeOptionalWithMax(String value, int maxLength, String errorKey) {
    String normalized = normalizeOptional(value);
    if (normalized == null) return null;
    if (normalized.length() > maxLength) {
      throw new IllegalArgumentException(errorKey);
    }
    return normalized;
  }

  private String normalizeTipoRegistro(String tipoRegistro) {
    if (tipoRegistro == null || tipoRegistro.isBlank()) {
      throw new IllegalArgumentException("pessoa_tipo_registro_required");
    }
    String normalized = tipoRegistro.trim().toUpperCase();
    if (!normalized.equals("CPF") && !normalized.equals("CNPJ") && !normalized.equals("ID_ESTRANGEIRO")) {
      throw new IllegalArgumentException("pessoa_tipo_registro_invalid");
    }
    return normalized;
  }

  private String normalizeRegistroFederal(String tipoRegistro, String registroFederal) {
    if (registroFederal == null || registroFederal.isBlank()) {
      throw new IllegalArgumentException("pessoa_registro_federal_required");
    }
    String value = registroFederal.trim();
    if ("CPF".equals(tipoRegistro) || "CNPJ".equals(tipoRegistro)) {
      String digits = value.replaceAll("\\D", "");
      if (!CpfCnpjValidator.isValid(digits)) {
        throw new IllegalArgumentException("pessoa_registro_federal_invalido");
      }
      return digits;
    }
    String upper = value.toUpperCase();
    if (upper.length() > 40) {
      throw new IllegalArgumentException("pessoa_registro_federal_too_long");
    }
    return upper;
  }

  private String normalizeTipoPessoa(String tipoPessoa, String tipoRegistro) {
    String defaultTipo = toTipoPessoa(tipoRegistro);
    if (tipoPessoa == null || tipoPessoa.isBlank()) {
      return defaultTipo;
    }
    String normalized = tipoPessoa.trim().toUpperCase();
    if (!normalized.equals("FISICA") && !normalized.equals("JURIDICA") && !normalized.equals("ESTRANGEIRA")) {
      throw new IllegalArgumentException("tipo_pessoa_invalido");
    }
    return normalized;
  }

  private LocalDate parseDate(String value, String errorKey) {
    String normalized = normalizeOptional(value);
    if (normalized == null) return null;
    try {
      return LocalDate.parse(normalized);
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException(errorKey);
    }
  }

  private String toTipoPessoa(String tipoRegistro) {
    return switch (tipoRegistro) {
      case "CPF" -> "FISICA";
      case "CNPJ" -> "JURIDICA";
      case "ID_ESTRANGEIRO" -> "ESTRANGEIRA";
      default -> throw new IllegalArgumentException("pessoa_tipo_registro_invalid");
    };
  }
}
