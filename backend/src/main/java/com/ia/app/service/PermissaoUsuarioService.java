package com.ia.app.service;

import com.ia.app.domain.Papel;
import com.ia.app.repository.PapelPermissaoRepository;
import com.ia.app.repository.PapelRepository;
import com.ia.app.repository.PermissaoCatalogoRepository;
import com.ia.app.repository.UsuarioPapelRepository;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class PermissaoUsuarioService {

  private final UsuarioPapelRepository usuarioPapelRepository;
  private final PapelPermissaoRepository papelPermissaoRepository;
  private final PapelRepository papelRepository;
  private final PermissaoCatalogoRepository permissaoCatalogoRepository;

  public PermissaoUsuarioService(UsuarioPapelRepository usuarioPapelRepository,
      PapelPermissaoRepository papelPermissaoRepository,
      PapelRepository papelRepository,
      PermissaoCatalogoRepository permissaoCatalogoRepository) {
    this.usuarioPapelRepository = usuarioPapelRepository;
    this.papelPermissaoRepository = papelPermissaoRepository;
    this.papelRepository = papelRepository;
    this.permissaoCatalogoRepository = permissaoCatalogoRepository;
  }

  @Cacheable(value = "permissoesUsuario", key = "#tenantId + ':' + #usuarioId")
  public Set<String> permissoes(Long tenantId, String usuarioId) {
    List<Long> papelIds = usuarioPapelRepository.findPapelIdsByUsuario(tenantId, usuarioId);
    if (papelIds.isEmpty()) {
      return Collections.emptySet();
    }
    List<Long> ativos = papelRepository.findAllById(papelIds).stream()
      .filter(Papel::isAtivo)
      .map(Papel::getId)
      .toList();
    if (ativos.isEmpty()) {
      return Collections.emptySet();
    }
    Set<String> permissoes = papelPermissaoRepository.findPermissoesByPapelIds(tenantId, ativos)
      .stream().collect(Collectors.toSet());
    if (permissoes.isEmpty()) return Collections.emptySet();
    Set<String> ativas = new java.util.HashSet<>(
      permissaoCatalogoRepository.findAllByTenantIdAndAtivoTrue(tenantId)
        .stream().map(p -> p.getCodigo()).toList()
    );
    permissoes.retainAll(ativas);
    return permissoes;
  }

  @Cacheable(value = "papeisUsuario", key = "#tenantId + ':' + #usuarioId")
  public List<String> papeis(Long tenantId, String usuarioId) {
    List<Long> papelIds = usuarioPapelRepository.findPapelIdsByUsuario(tenantId, usuarioId);
    if (papelIds.isEmpty()) {
      return List.of();
    }
    return papelRepository.findAllById(papelIds).stream()
      .filter(Papel::isAtivo)
      .map(Papel::getNome)
      .sorted()
      .toList();
  }
}
