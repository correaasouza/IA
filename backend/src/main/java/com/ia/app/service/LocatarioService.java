package com.ia.app.service;

import com.ia.app.domain.Locatario;
import com.ia.app.domain.Usuario;
import com.ia.app.dto.LocatarioRequest;
import com.ia.app.dto.LocatarioResponse;
import com.ia.app.mapper.LocatarioMapper;
import com.ia.app.repository.LocatarioRepository;
import com.ia.app.repository.UsuarioLocatarioAcessoRepository;
import com.ia.app.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

@Service
public class LocatarioService {

  private final LocatarioRepository repository;
  private final UsuarioRepository usuarioRepository;
  private final UsuarioLocatarioAcessoRepository usuarioLocatarioAcessoRepository;
  private final PermissaoCatalogService permissaoCatalogService;
  private final PapelSeedService papelSeedService;
  private final TenantAdminSeedService tenantAdminSeedService;
  private final TipoEntidadeSeedService tipoEntidadeSeedService;
  private final MovimentoConfigSeedService movimentoConfigSeedService;
  private final TenantUnitService tenantUnitService;

  public LocatarioService(LocatarioRepository repository,
      PermissaoCatalogService permissaoCatalogService,
      PapelSeedService papelSeedService,
      TenantAdminSeedService tenantAdminSeedService,
      TipoEntidadeSeedService tipoEntidadeSeedService,
      MovimentoConfigSeedService movimentoConfigSeedService,
      TenantUnitService tenantUnitService,
      UsuarioRepository usuarioRepository,
      UsuarioLocatarioAcessoRepository usuarioLocatarioAcessoRepository) {
    this.repository = repository;
    this.permissaoCatalogService = permissaoCatalogService;
    this.papelSeedService = papelSeedService;
    this.tenantAdminSeedService = tenantAdminSeedService;
    this.tipoEntidadeSeedService = tipoEntidadeSeedService;
    this.movimentoConfigSeedService = movimentoConfigSeedService;
    this.tenantUnitService = tenantUnitService;
    this.usuarioRepository = usuarioRepository;
    this.usuarioLocatarioAcessoRepository = usuarioLocatarioAcessoRepository;
  }

  public Page<Locatario> findAll(String nome, Boolean ativo, Boolean bloqueado, Pageable pageable) {
    Specification<Locatario> spec = Specification
      .where(com.ia.app.repository.LocatarioSpecifications.nomeLike(nome))
      .and(com.ia.app.repository.LocatarioSpecifications.ativoEquals(ativo))
      .and(com.ia.app.repository.LocatarioSpecifications.bloqueadoEquals(bloqueado));
    return repository.findAll(spec, pageable);
  }

  public Locatario create(LocatarioRequest request) {
    Locatario locatario = new Locatario();
    locatario.setNome(request.nome());
    locatario.setDataLimiteAcesso(request.dataLimiteAcesso());
    locatario.setAtivo(request.ativo());
    Locatario saved = repository.save(locatario);
    permissaoCatalogService.seedDefaults(saved.getId());
    papelSeedService.seedDefaults(saved.getId());
    tipoEntidadeSeedService.seedDefaults(saved.getId());
    movimentoConfigSeedService.seedDefaults(saved.getId());
    tenantUnitService.seedMissingMirrorsForTenant(saved.getId());
    tenantAdminSeedService.seedDefaultAdmin(saved);
    return saved;
  }

  public Locatario update(Long id, LocatarioRequest request) {
    Locatario locatario = repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("locatario_not_found"));
    locatario.setNome(request.nome());
    locatario.setDataLimiteAcesso(request.dataLimiteAcesso());
    locatario.setAtivo(request.ativo());
    return repository.save(locatario);
  }

  public Locatario updateAccessLimit(Long id, LocalDate dataLimiteAcesso) {
    Locatario locatario = repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("locatario_not_found"));
    locatario.setDataLimiteAcesso(dataLimiteAcesso);
    return repository.save(locatario);
  }

  public Locatario updateStatus(Long id, boolean ativo) {
    Locatario locatario = repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("locatario_not_found"));
    locatario.setAtivo(ativo);
    return repository.save(locatario);
  }

  public Locatario getById(Long id) {
    return repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("locatario_not_found"));
  }

  public void delete(Long id) {
    Locatario locatario = repository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("locatario_not_found"));
    locatario.setAtivo(false);
    locatario.setDataLimiteAcesso(LocalDate.now().minusDays(1));
    repository.save(locatario);
  }

  public List<LocatarioResponse> findAllowed(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return List.of();
    }
    String keycloakId = authentication.getName();
    String preferredUsername = null;
    if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
      keycloakId = jwtAuth.getToken().getSubject();
      preferredUsername = jwtAuth.getToken().getClaimAsString("preferred_username");
    }
    boolean isMaster = authentication.getAuthorities().stream()
      .anyMatch(a -> a.getAuthority().equals("ROLE_MASTER"));
    boolean isGlobalMaster = isMaster
      && ((preferredUsername != null && preferredUsername.equalsIgnoreCase("master"))
        || "master".equalsIgnoreCase(keycloakId));
    if (isGlobalMaster) {
      return repository.findAll().stream().map(LocatarioMapper::toResponse).toList();
    }
    if (preferredUsername != null && preferredUsername.equalsIgnoreCase("master")) {
      return repository.findById(1L).stream()
        .map(LocatarioMapper::toResponse).toList();
    }
    java.util.List<Long> allowedIds = usuarioLocatarioAcessoRepository.findLocatarioIdsByUsuarioId(keycloakId);
    if (allowedIds.isEmpty()) {
      Usuario usuario = usuarioRepository.findByKeycloakId(keycloakId).orElse(null);
      if (usuario == null) {
        return List.of();
      }
      allowedIds = List.of(usuario.getTenantId());
    }
    if (allowedIds.isEmpty()) {
      return List.of();
    }
    return repository.findAllById(allowedIds).stream()
      .map(LocatarioMapper::toResponse)
      .toList();
  }
}

