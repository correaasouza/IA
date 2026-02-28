package com.ia.app.dto;

import java.util.List;

public record EntidadeRhOptionsResponse(
  List<EntidadeRhOptionResponse> frequenciasCobranca,
  List<EntidadeRhOptionResponse> tiposFuncionario,
  List<EntidadeRhOptionResponse> situacoesFuncionario,
  List<EntidadeRhOptionResponse> setores,
  List<EntidadeRhOptionResponse> cargos,
  List<EntidadeRhOptionResponse> ocupacoesAtividade,
  List<EntidadeRhOptionResponse> qualificacoes
) {}
