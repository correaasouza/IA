package com.ia.app.service;

import com.ia.app.domain.Locatario;
import com.ia.app.domain.Usuario;
import com.ia.app.dto.LocatarioRequest;
import com.ia.app.dto.LocatarioResponse;
import com.ia.app.mapper.LocatarioMapper;
import com.ia.app.repository.LocatarioRepository;
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
  private final EntidadeDefinicaoService entidadeDefinicaoService;
  private final TipoEntidadeService tipoEntidadeService;
  private final ContatoTipoService contatoTipoService;
  private final PermissaoCatalogService permissaoCatalogService;
  private final PapelSeedService papelSeedService;
  private final TenantAdminSeedService tenantAdminSeedService;

  public LocatarioService(LocatarioRepository repository, EntidadeDefinicaoService entidadeDefinicaoService,
      TipoEntidadeService tipoEntidadeService,
      ContatoTipoService contatoTipoService,
      PermissaoCatalogService permissaoCatalogService,
      PapelSeedService papelSeedService,
      TenantAdminSeedService tenantAdminSeedService,
      UsuarioRepository usuarioRepository) {
    this.repository = repository;
    this.entidadeDefinicaoService = entidadeDefinicaoService;
    this.tipoEntidadeService = tipoEntidadeService;
    this.contatoTipoService = contatoTipoService;
    this.permissaoCatalogService = permissaoCatalogService;
    this.papelSeedService = papelSeedService;
    this.tenantAdminSeedService = tenantAdminSeedService;
    this.usuarioRepository = usuarioRepository;
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
    entidadeDefinicaoService.seedDefaults(saved.getId());
    tipoEntidadeService.seedDefaults(saved.getId());
    contatoTipoService.seedDefaults(saved.getId());
    permissaoCatalogService.seedDefaults(saved.getId());
    papelSeedService.seedDefaults(saved.getId());
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
    boolean isMaster = authentication.getAuthorities().stream()
      .anyMatch(a -> a.getAuthority().equals("ROLE_MASTER_ADMIN"));
    if (isMaster) {
      return repository.findAll().stream().map(LocatarioMapper::toResponse).toList();
    }
    String keycloakId = authentication.getName();
    String preferredUsername = null;
    if (authentication instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken jwtAuth) {
      keycloakId = jwtAuth.getToken().getSubject();
      preferredUsername = jwtAuth.getToken().getClaimAsString("preferred_username");
    }
    if (preferredUsername != null && preferredUsername.equalsIgnoreCase("master")) {
      return repository.findById(1L).stream()
        .map(LocatarioMapper::toResponse).toList();
    }
    Usuario usuario = usuarioRepository.findByKeycloakId(keycloakId).orElse(null);
    if (usuario == null) {
      return List.of();
    }
    return repository.findById(usuario.getTenantId()).stream()
      .map(LocatarioMapper::toResponse).toList();
  }
}
