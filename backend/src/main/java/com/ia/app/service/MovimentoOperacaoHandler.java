package com.ia.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.ia.app.domain.MovimentoTipo;
import com.ia.app.dto.MovimentoTemplateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MovimentoOperacaoHandler {

  MovimentoTipo supports();

  Object buildTemplate(MovimentoTemplateRequest request);

  Object create(JsonNode payload);

  Page<?> list(Pageable pageable, String nome);

  Object get(Long id);

  Object update(Long id, JsonNode payload);

  void delete(Long id);
}
