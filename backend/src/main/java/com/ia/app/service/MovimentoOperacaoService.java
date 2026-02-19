package com.ia.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ia.app.domain.MovimentoTipo;
import com.ia.app.dto.MovimentoTemplateRequest;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MovimentoOperacaoService {

  private final Map<MovimentoTipo, MovimentoOperacaoHandler> handlers = new EnumMap<>(MovimentoTipo.class);

  public MovimentoOperacaoService(List<MovimentoOperacaoHandler> handlerList) {
    for (MovimentoOperacaoHandler handler : handlerList) {
      handlers.put(handler.supports(), handler);
    }
  }

  public Object buildTemplate(MovimentoTipo tipo, MovimentoTemplateRequest request) {
    return requireHandler(tipo).buildTemplate(request);
  }

  public Object create(MovimentoTipo tipo, JsonNode payload) {
    return requireHandler(tipo).create(payload);
  }

  public Page<?> list(
      MovimentoTipo tipo,
      Pageable pageable,
      String nome) {
    return requireHandler(tipo).list(pageable, nome);
  }

  public Object get(MovimentoTipo tipo, Long id) {
    return requireHandler(tipo).get(id);
  }

  public Object update(MovimentoTipo tipo, Long id, JsonNode payload) {
    return requireHandler(tipo).update(id, payload);
  }

  public void delete(MovimentoTipo tipo, Long id) {
    requireHandler(tipo).delete(id);
  }

  private MovimentoOperacaoHandler requireHandler(MovimentoTipo tipo) {
    MovimentoOperacaoHandler handler = handlers.get(tipo);
    if (handler == null) {
      throw new IllegalArgumentException("movimento_tipo_nao_implementado");
    }
    return handler;
  }
}
