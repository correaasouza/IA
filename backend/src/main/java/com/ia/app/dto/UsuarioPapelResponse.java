package com.ia.app.dto;

import java.util.List;

public record UsuarioPapelResponse(
  List<Long> papelIds,
  List<String> papeis
) {}
