package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;

public record GrantTenantAccessRequest(
    @NotNull Long userId,
    @NotNull Long tenantId
) {}
