package com.ia.app.security;

import com.ia.app.domain.Locatario;
import com.ia.app.repository.LocatarioRepository;
import com.ia.app.repository.UsuarioLocatarioAcessoRepository;
import com.ia.app.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ia.app.tenant.TenantContext;

@Component
public class TenantAccessFilter extends OncePerRequestFilter {

  private static final Set<String> OPEN_PATHS = Set.of(
    "/actuator/health",
    "/api/me",
    "/api/locatarios/allowed"
  );

  private final LocatarioRepository repository;
  private final UsuarioRepository usuarioRepository;
  private final UsuarioLocatarioAcessoRepository usuarioLocatarioAcessoRepository;
  private final ObjectMapper objectMapper;

  public TenantAccessFilter(
      LocatarioRepository repository,
      UsuarioRepository usuarioRepository,
      UsuarioLocatarioAcessoRepository usuarioLocatarioAcessoRepository,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.usuarioRepository = usuarioRepository;
    this.usuarioLocatarioAcessoRepository = usuarioLocatarioAcessoRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    if (HttpMethod.OPTIONS.matches(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }

    String path = request.getRequestURI();
    if (isOpenPath(path)) {
      filterChain.doFilter(request, response);
      return;
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    boolean isMaster = authentication != null && hasRole(authentication, "ROLE_MASTER");

    String tenantIdHeader = request.getHeader("X-Tenant-Id");
    if ((tenantIdHeader == null || tenantIdHeader.isBlank()) && !isMaster) {
      writeProblem(response, 400, "tenant_required", "Tenant requerido",
        "Informe o header X-Tenant-Id.");
      return;
    }

    if (tenantIdHeader == null || tenantIdHeader.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }

    Long tenantId;
    try {
      tenantId = Long.parseLong(tenantIdHeader);
    } catch (NumberFormatException ex) {
      writeProblem(response, 400, "tenant_invalid", "Tenant inválido",
        "X-Tenant-Id deve ser um número.");
      return;
    }

    Locatario locatario = repository.findById(tenantId).orElse(null);
    if (locatario == null) {
      writeProblem(response, 404, "tenant_not_found", "Locatário não encontrado",
        "Locatário informado não existe.");
      return;
    }

    boolean bloqueado = LocalDate.now().isAfter(locatario.getDataLimiteAcesso()) || !locatario.isAtivo();
    if (bloqueado && !isMaster) {
      writeProblem(response, 423, "tenant_blocked", "Locatário bloqueado",
        "Data limite expirada ou locatário inativo. Entre em contato com o suporte.",
        locatario.getId(), locatario.getDataLimiteAcesso().toString(), locatario.isAtivo());
      return;
    }

    if (!isMaster && authentication != null) {
      String keycloakId = authentication.getName();
      if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
        keycloakId = jwtAuth.getToken().getSubject();
      }
      boolean allowed = usuarioLocatarioAcessoRepository.existsByUsuarioIdAndLocatarioId(keycloakId, tenantId)
        || usuarioRepository.findByKeycloakIdAndTenantId(keycloakId, tenantId).isPresent();
      if (!allowed) {
        writeProblem(response, 403, "tenant_forbidden", "Acesso negado",
          "Usuário não pertence ao locatário informado.");
        return;
      }
    }

    try {
      TenantContext.setTenantId(tenantId);
      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }

  private boolean isOpenPath(String path) {
    if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
      return true;
    }
    return OPEN_PATHS.contains(path);
  }

  private boolean hasRole(Authentication authentication, String role) {
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      if (role.equals(authority.getAuthority())) {
        return true;
      }
    }
    return false;
  }

  private void writeProblem(HttpServletResponse response, int status, String type,
      String title, String detail) throws IOException {
    writeProblem(response, status, type, title, detail, null, null, null);
  }

  private void writeProblem(HttpServletResponse response, int status, String type,
      String title, String detail, Long tenantId, String dataLimiteAcesso, Boolean ativo) throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    var body = new java.util.LinkedHashMap<String, Object>();
    body.put("type", type);
    body.put("title", title);
    body.put("status", status);
    body.put("detail", detail);
    if (tenantId != null) {
      body.put("tenantId", tenantId);
    }
    if (dataLimiteAcesso != null) {
      body.put("dataLimiteAcesso", dataLimiteAcesso);
    }
    if (ativo != null) {
      body.put("ativo", ativo);
    }
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}


