package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;

public record GrantCompanyAccessRequest(
    @NotNull Long userId,
    @NotNull Long companyId
) {}
