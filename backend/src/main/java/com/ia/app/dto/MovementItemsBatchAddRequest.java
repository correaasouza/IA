package com.ia.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MovementItemsBatchAddRequest(
  @NotEmpty List<@Valid MovementItemAddRequest> items
) {}