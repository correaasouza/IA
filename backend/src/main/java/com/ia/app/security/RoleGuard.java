package com.ia.app.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class RoleGuard {

  public boolean hasRole(String role) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return false;
    }
    return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
  }
}
