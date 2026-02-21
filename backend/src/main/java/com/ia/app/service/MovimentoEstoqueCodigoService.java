package com.ia.app.service;

import com.ia.app.domain.MovimentoEstoqueCodigoSeq;
import com.ia.app.repository.MovimentoConfigRepository;
import com.ia.app.repository.MovimentoEstoqueCodigoSeqRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MovimentoEstoqueCodigoService {

  private final MovimentoEstoqueCodigoSeqRepository repository;
  private final MovimentoConfigRepository movimentoConfigRepository;

  public MovimentoEstoqueCodigoService(
      MovimentoEstoqueCodigoSeqRepository repository,
      MovimentoConfigRepository movimentoConfigRepository) {
    this.repository = repository;
    this.movimentoConfigRepository = movimentoConfigRepository;
  }

  @Transactional
  public Long proximoCodigo(Long tenantId, Long movimentoConfigId) {
    if (tenantId == null || movimentoConfigId == null) {
      throw new IllegalArgumentException("movimento_estoque_codigo_scope_invalid");
    }

    movimentoConfigRepository.findByIdAndTenantId(movimentoConfigId, tenantId)
      .orElseThrow(() -> new IllegalArgumentException("movimento_estoque_codigo_scope_invalid"));

    MovimentoEstoqueCodigoSeq seq = repository
      .findWithLockByTenantIdAndMovimentoConfigId(tenantId, movimentoConfigId)
      .orElseGet(() -> criarInicial(tenantId, movimentoConfigId));

    if (seq.getNextValue() == null || seq.getNextValue() < 1) {
      seq.setNextValue(1L);
    }
    Long codigo = seq.getNextValue();
    seq.setNextValue(codigo + 1);
    repository.save(seq);
    return codigo;
  }

  private MovimentoEstoqueCodigoSeq criarInicial(Long tenantId, Long movimentoConfigId) {
    MovimentoEstoqueCodigoSeq seq = new MovimentoEstoqueCodigoSeq();
    seq.setTenantId(tenantId);
    seq.setMovimentoConfigId(movimentoConfigId);
    seq.setNextValue(1L);
    try {
      return repository.save(seq);
    } catch (DataIntegrityViolationException ex) {
      return repository.findWithLockByTenantIdAndMovimentoConfigId(tenantId, movimentoConfigId)
        .orElseThrow(() -> ex);
    }
  }
}
