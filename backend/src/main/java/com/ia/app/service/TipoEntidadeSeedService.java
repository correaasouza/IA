package com.ia.app.service;

import com.ia.app.domain.TipoEntidade;
import com.ia.app.repository.TipoEntidadeRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TipoEntidadeSeedService {

  private static final List<String> DEFAULT_CODES = List.of("CLIENTE", "FORNECEDOR", "EQUIPE");

  private final TipoEntidadeRepository tipoEntidadeRepository;

  public TipoEntidadeSeedService(TipoEntidadeRepository tipoEntidadeRepository) {
    this.tipoEntidadeRepository = tipoEntidadeRepository;
  }

  @Transactional
  public void seedDefaults(Long tenantId) {
    for (String code : DEFAULT_CODES) {
      if (tipoEntidadeRepository.findByTenantIdAndCodigoSeed(tenantId, code).isPresent()) {
        continue;
      }
      TipoEntidade entity = new TipoEntidade();
      entity.setTenantId(tenantId);
      entity.setNome(code);
      entity.setCodigoSeed(code);
      entity.setTipoPadrao(true);
      entity.setAtivo(true);
      try {
        tipoEntidadeRepository.save(entity);
      } catch (DataIntegrityViolationException ex) {
        if (tipoEntidadeRepository.findByTenantIdAndCodigoSeed(tenantId, code).isEmpty()) {
          throw ex;
        }
      }
    }
  }
}
