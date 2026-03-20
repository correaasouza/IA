package com.ia.app.dto;

import jakarta.validation.constraints.NotNull;

public record SetDefaultCompanyRequest(@NotNull Long companyId) {}
