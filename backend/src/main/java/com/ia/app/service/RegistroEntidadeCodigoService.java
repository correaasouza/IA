package com.ia.app.service;

import com.ia.app.domain.RegistroEntidadeCodigoSeq;
import com.ia.app.repository.RegistroEntidadeCodigoSeqRepository;
import com.ia.app.repository.TipoEntidadeConfigPorAgrupadorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroEntidadeCodigoService {

  private final RegistroEntidadeCodigoSeqRepository repository;
  private final TipoEntidadeConfigPorAgrupadorRepository configRepository;

  public RegistroEntidadeCodigoService(
      RegistroEntidadeCodigoSeqRepository repository,
      TipoEntidadeConfigPorAgrupadorRepository configRepository) {
    this.repository = repository;
    this.configRepository = configRepository;
  }

  @Transactional
  public Long proximoCodigo(Long tenantId, Long tipoEntidadeConfigAgrupadorId) {
    if (tenantId == null || tipoEntidadeConfigAgrupadorId == null) {
      throw new IllegalArgumentException("registro_entidade_codigo_scope_invalid");
    }

    configRepository.findWithLockByIdAndTenantIdAndAtivoTrue(tipoEntidadeConfigAgrupadorId, tenantId)
      .orElseThrow(() -> new IllegalArgumentException("tipo_entidade_config_agrupador_not_found"));

    RegistroEntidadeCodigoSeq seq = repository
      .findWithLockByTenantIdAndTipoEntidadeConfigAgrupadorId(tenantId, tipoEntidadeConfigAgrupadorId)
      .orElseGet(() -> criarInicial(tenantId, tipoEntidadeConfigAgrupadorId));

    if (seq.getNextValue() == null || seq.getNextValue() < 1) {
      seq.setNextValue(1L);
    }
    Long codigo = seq.getNextValue();
    seq.setNextValue(codigo + 1);
    repository.save(seq);
    return codigo;
  }

  private RegistroEntidadeCodigoSeq criarInicial(Long tenantId, Long tipoEntidadeConfigAgrupadorId) {
    RegistroEntidadeCodigoSeq seq = new RegistroEntidadeCodigoSeq();
    seq.setTenantId(tenantId);
    seq.setTipoEntidadeConfigAgrupadorId(tipoEntidadeConfigAgrupadorId);
    seq.setNextValue(1L);
    try {
      return repository.save(seq);
    } catch (DataIntegrityViolationException ex) {
      return repository.findWithLockByTenantIdAndTipoEntidadeConfigAgrupadorId(tenantId, tipoEntidadeConfigAgrupadorId)
        .orElseThrow(() -> ex);
    }
  }
}
