package com.ia.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.app.domain.Locatario;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.repository.LocatarioRepository;
import com.ia.app.tenant.EmpresaContext;
import com.ia.app.tenant.TenantContext;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TenantAccessFilter extends OncePerRequestFilter {

  private static final Set<String> OPEN_PATHS = Set.of(
    "/actuator/health",
    "/api/me",
    "/api/locatarios/allowed"
  );

  private final LocatarioRepository locatarioRepository;
  private final EmpresaRepository empresaRepository;
  private final AuthorizationService authorizationService;
  private final ObjectMapper objectMapper;

  public TenantAccessFilter(
      LocatarioRepository locatarioRepository,
      EmpresaRepository empresaRepository,
      AuthorizationService authorizationService,
      ObjectMapper objectMapper) {
    this.locatarioRepository = locatarioRepository;
    this.empresaRepository = empresaRepository;
    this.authorizationService = authorizationService;
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

    String tenantIdHeader = request.getHeader("X-Tenant-Id");
    if (tenantIdHeader == null || tenantIdHeader.isBlank()) {
      writeProblem(response, 400, "tenant_required", "Tenant requerido",
        "Informe o header X-Tenant-Id.");
      return;
    }

    Long tenantId;
    try {
      tenantId = Long.parseLong(tenantIdHeader);
    } catch (NumberFormatException ex) {
      writeProblem(response, 400, "tenant_invalid", "Tenant invalido",
        "X-Tenant-Id deve ser um numero.");
      return;
    }

    Locatario locatario = locatarioRepository.findById(tenantId).orElse(null);
    if (locatario == null) {
      writeProblem(response, 404, "tenant_not_found", "Locatario nao encontrado",
        "Locatario informado nao existe.");
      return;
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    boolean globalMaster = authorizationService.isGlobalMaster(authentication, tenantId);
    String userId = resolveUserId(authentication);

    boolean bloqueado = LocalDate.now().isAfter(locatario.getDataLimiteAcesso()) || !locatario.isAtivo();
    if (bloqueado && !globalMaster) {
      writeProblem(response, 423, "tenant_blocked", "Locatario bloqueado",
        "Data limite expirada ou locatario inativo. Entre em contato com o suporte.",
        locatario.getId(), locatario.getDataLimiteAcesso().toString(), locatario.isAtivo());
      return;
    }

    if (!authorizationService.canAccessTenant(userId, tenantId)) {
      writeProblem(response, 403, "tenant_forbidden", "Acesso negado",
        "Usuario nao pertence ao locatario informado.");
      return;
    }

    String empresaIdHeader = shouldIgnoreEmpresaHeader(request)
      ? null
      : request.getHeader("X-Empresa-Id");
    Long empresaId = null;
    if (empresaIdHeader != null && !empresaIdHeader.isBlank()) {
      try {
        empresaId = Long.parseLong(empresaIdHeader);
      } catch (NumberFormatException ex) {
        writeProblem(response, 400, "empresa_context_invalid", "Empresa invalida",
          "X-Empresa-Id deve ser um numero.");
        return;
      }
      if (empresaId <= 0 || !empresaRepository.existsByIdAndTenantId(empresaId, tenantId)) {
        writeProblem(response, 404, "empresa_context_not_found", "Empresa nao encontrada",
          "Empresa informada nao existe no locatario.");
        return;
      }
      if (!globalMaster && !authorizationService.getAccessibleCompanies(userId, tenantId).contains(empresaId)) {
        writeProblem(response, 403, "empresa_context_forbidden", "Empresa sem acesso",
          "Usuario sem acesso a empresa informada.");
        return;
      }
    }

    try {
      TenantContext.setTenantId(tenantId);
      if (empresaId != null) {
        EmpresaContext.setEmpresaId(empresaId);
      } else {
        EmpresaContext.clear();
      }
      filterChain.doFilter(request, response);
    } finally {
      EmpresaContext.clear();
      TenantContext.clear();
    }
  }

  private boolean isOpenPath(String path) {
    if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
      return true;
    }
    return OPEN_PATHS.contains(path);
  }

  private boolean shouldIgnoreEmpresaHeader(HttpServletRequest request) {
    String method = request.getMethod();
    String path = request.getRequestURI();
    // Company list endpoints are used to choose context; stale X-Empresa-Id must not block them.
    return HttpMethod.GET.matches(method) && path != null && path.startsWith("/api/empresas");
  }

  private String resolveUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
      return jwtAuth.getToken().getSubject();
    }
    return authentication.getName();
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
