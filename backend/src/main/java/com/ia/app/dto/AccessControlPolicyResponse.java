package com.ia.app.dto;

import java.util.List;

public record AccessControlPolicyResponse(
  String controlKey,
  List<String> roles
) {}

