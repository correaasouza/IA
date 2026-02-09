package com.ia.app.service;

import com.ia.app.domain.CampoDefinicao;
import com.ia.app.domain.RegistroCampoValor;
import com.ia.app.domain.RegistroEntidade;
import com.ia.app.dto.RegistroEntidadeRequest;
import com.ia.app.repository.CampoDefinicaoRepository;
import com.ia.app.repository.RegistroCampoValorRepository;
import com.ia.app.repository.RegistroEntidadeRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class RegistroEntidadeService {

  private final RegistroEntidadeRepository registroRepository;
  private final RegistroCampoValorRepository valorRepository;
  private final CampoDefinicaoRepository campoRepository;

  public RegistroEntidadeService(RegistroEntidadeRepository registroRepository,
      RegistroCampoValorRepository valorRepository,
      CampoDefinicaoRepository campoRepository) {
    this.registroRepository = registroRepository;
    this.valorRepository = valorRepository;
    this.campoRepository = campoRepository;
  }

  public Page<RegistroEntidade> list(Long tipoEntidadeId, Pageable pageable) {
    Long tenantId = requireTenant();
    return registroRepository.findAllByTenantIdAndTipoEntidadeId(tenantId, tipoEntidadeId, pageable);
  }

  public Map<Long, Map<String, Object>> loadValores(List<Long> registroIds) {
    Long tenantId = requireTenant();
    List<RegistroCampoValor> valores = valorRepository.findAllByTenantIdAndRegistroEntidadeIdIn(tenantId, registroIds);
    Map<Long, Map<String, Object>> result = new HashMap<>();
    for (RegistroCampoValor valor : valores) {
      result.computeIfAbsent(valor.getRegistroEntidadeId(), id -> new HashMap<>())
        .put(String.valueOf(valor.getCampoDefinicaoId()), toValue(valor));
    }
    return result;
  }

  public RegistroEntidade create(RegistroEntidadeRequest request) {
    Long tenantId = requireTenant();
    List<CampoDefinicao> campos = campoRepository
      .findAllByTenantIdAndTipoEntidadeId(tenantId, request.tipoEntidadeId(), Pageable.unpaged())
      .getContent();
    if (campos.isEmpty()) {
      throw new EntityNotFoundException("campo_definicao_not_found");
    }

    RegistroEntidade registro = new RegistroEntidade();
    registro.setTenantId(tenantId);
    registro.setTipoEntidadeId(request.tipoEntidadeId());
    registro = registroRepository.save(registro);

    Map<String, Object> valores = request.valores() == null ? Map.of() : request.valores();
    for (CampoDefinicao campo : campos) {
      Object raw = valores.get(String.valueOf(campo.getId()));
      if (campo.isObrigatorio() && raw == null) {
        throw new IllegalArgumentException("campo_obrigatorio: " + campo.getNome());
      }
      RegistroCampoValor valor = new RegistroCampoValor();
      valor.setTenantId(tenantId);
      valor.setRegistroEntidadeId(registro.getId());
      valor.setCampoDefinicaoId(campo.getId());
      applyValue(valor, campo.getTipo(), raw);
      valorRepository.save(valor);
    }

    return registro;
  }

  private void applyValue(RegistroCampoValor valor, String tipo, Object raw) {
    if (raw == null) {
      return;
    }
    switch (tipo.toUpperCase()) {
      case "TEXTO" -> valor.setValorTexto(String.valueOf(raw));
      case "NUMERO" -> valor.setValorNumero(new BigDecimal(String.valueOf(raw)));
      case "DATA" -> valor.setValorData(LocalDate.parse(String.valueOf(raw)));
      case "BOOLEANO" -> valor.setValorBooleano(Boolean.valueOf(String.valueOf(raw)));
      default -> valor.setValorTexto(String.valueOf(raw));
    }
  }

  private Object toValue(RegistroCampoValor valor) {
    if (valor.getValorTexto() != null) return valor.getValorTexto();
    if (valor.getValorNumero() != null) return valor.getValorNumero();
    if (valor.getValorData() != null) return valor.getValorData().toString();
    if (valor.getValorBooleano() != null) return valor.getValorBooleano();
    return null;
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
