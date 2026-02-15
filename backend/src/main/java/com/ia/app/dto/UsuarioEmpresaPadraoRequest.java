package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;

public record UsuarioEmpresaPadraoRequest(
    @NotNull Long empresaId
) {
}

