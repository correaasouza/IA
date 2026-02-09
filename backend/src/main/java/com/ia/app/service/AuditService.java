package com.ia.app.service;

import com.ia.app.domain.AuditoriaEvento;
import com.ia.app.repository.AuditoriaEventoRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

  private final AuditoriaEventoRepository repository;

  public AuditService(AuditoriaEventoRepository repository) {
    this.repository = repository;
  }

  public void log(Long tenantId, String tipo, String entidade, String entidadeId, String dados) {
    AuditoriaEvento e = new AuditoriaEvento();
    e.setTenantId(tenantId);
    e.setTipo(tipo);
    e.setEntidade(entidade);
    e.setEntidadeId(entidadeId);
    e.setDados(dados);
    repository.save(e);
  }
}
