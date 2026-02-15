package com.ia.app.service;

import com.ia.app.domain.Pessoa;
import com.ia.app.dto.PessoaVinculoRequest;
import com.ia.app.repository.PessoaRepository;
import com.ia.app.util.CpfCnpjValidator;
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
            registroFederalNormalizado)));
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

  private Pessoa buildPessoa(
      Long tenantId,
      String nome,
      String apelido,
      String tipoRegistro,
      String registroFederalNormalizado) {
    Pessoa entity = new Pessoa();
    entity.setTenantId(tenantId);
    entity.setNome(nome);
    entity.setApelido(apelido);
    entity.setTipoRegistro(tipoRegistro);
    entity.setRegistroFederal(registroFederalNormalizado);
    entity.setRegistroFederalNormalizado(registroFederalNormalizado);
    entity.setTipoPessoa(toTipoPessoa(tipoRegistro));
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

  private String toTipoPessoa(String tipoRegistro) {
    return switch (tipoRegistro) {
      case "CPF" -> "FISICA";
      case "CNPJ" -> "JURIDICA";
      case "ID_ESTRANGEIRO" -> "ESTRANGEIRA";
      default -> throw new IllegalArgumentException("pessoa_tipo_registro_invalid");
    };
  }
}
