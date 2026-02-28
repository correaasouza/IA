package com.ia.app.dto;

public record EntidadeDadosFiscaisResponse(
  Long id,
  Long registroEntidadeId,
  Short manifestarNotaAutomaticamente,
  Short usaNotaFiscalFatura,
  Short ignorarImportacaoNota,
  Long version
) {}
