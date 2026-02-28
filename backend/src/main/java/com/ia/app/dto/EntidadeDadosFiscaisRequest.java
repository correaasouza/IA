package com.ia.app.dto;

public record EntidadeDadosFiscaisRequest(
  Short manifestarNotaAutomaticamente,
  Short usaNotaFiscalFatura,
  Short ignorarImportacaoNota,
  Long version
) {}
