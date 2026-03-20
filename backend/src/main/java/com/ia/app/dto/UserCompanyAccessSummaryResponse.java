package com.ia.app.dto;

import java.util.List;

public record UserCompanyAccessSummaryResponse(
    List<Long> accessibleCompanies,
    Long defaultCompanyId,
    boolean canGrantCompanyAccess
) {}
