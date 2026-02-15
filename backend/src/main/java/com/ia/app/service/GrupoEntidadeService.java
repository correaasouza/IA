package com.ia.app.service;

import com.ia.app.domain.GrupoEntidade;
import com.ia.app.dto.GrupoEntidadeRequest;
import com.ia.app.dto.GrupoEntidadeResponse;
import com.ia.app.dto.GrupoEntidadeUpdateRequest;
import com.ia.app.repository.GrupoEntidadeRepository;
import com.ia.app.repository.RegistroEntidadeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GrupoEntidadeService {

  private final GrupoEntidadeRepository repository;
  private final RegistroEntidadeRepository registroRepository;
  private final RegistroEntidadeContextoService contextoService;
  private final AuditService auditService;

  public GrupoEntidadeService(
      GrupoEntidadeRepository repository,
      RegistroEntidadeRepository registroRepository,
      RegistroEntidadeContextoService contextoService,
      AuditService auditService) {
    this.repository = repository;
    this.registroRepository = registroRepository;
    this.contextoService = contextoService;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<GrupoEntidadeResponse> tree(Long tipoEntidadeId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    List<GrupoEntidade> groups = repository.findAllByTenantIdAndTipoEntidadeConfigAgrupadorIdAndAtivoTrueOrderByPathAsc(
      scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId());
    Map<Long, Long> directCountByGroupId = loadDirectCountByGroupId(scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId());
    return toTree(groups, directCountByGroupId);
  }

  @Transactional
  public GrupoEntidadeResponse create(Long tipoEntidadeId, GrupoEntidadeRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    String nome = normalizeNome(request.nome());
    String nomeNorm = normalizeNomeKey(nome);

    GrupoEntidade parent = resolveParent(
      scope.tenantId(),
      scope.tipoEntidadeConfigAgrupadorId(),
      request.parentId());
    Long parentId = parent == null ? null : parent.getId();
    int nivel = parent == null ? 0 : parent.getNivel() + 1;
    int ordem = (int) repository.countByTenantIdAndTipoEntidadeConfigAgrupadorIdAndParentIdAndAtivoTrue(
      scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId(), parentId) + 1;

    validateUniqueName(scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId(), parentId, nomeNorm, null);

    GrupoEntidade entity = new GrupoEntidade();
    entity.setTenantId(scope.tenantId());
    entity.setTipoEntidadeConfigAgrupadorId(scope.tipoEntidadeConfigAgrupadorId());
    entity.setParentId(parentId);
    entity.setNome(nome);
    entity.setNomeNormalizado(nomeNorm);
    entity.setNivel(nivel);
    entity.setOrdem(ordem);
    entity.setPath("TMP");
    entity.setAtivo(true);
    GrupoEntidade saved = repository.save(entity);

    String path = parent == null
      ? formatNode(saved.getId())
      : parent.getPath() + "/" + formatNode(saved.getId());
    saved.setPath(path);
    saved = repository.save(saved);

    auditService.log(scope.tenantId(), "GRUPO_ENTIDADE_CRIADO", "grupo_entidade", String.valueOf(saved.getId()),
      "tipoEntidadeId=" + tipoEntidadeId + ";configAgrupadorId=" + scope.tipoEntidadeConfigAgrupadorId()
        + ";nome=" + saved.getNome());
    return toResponse(saved);
  }

  @Transactional
  public GrupoEntidadeResponse update(Long tipoEntidadeId, Long grupoId, GrupoEntidadeUpdateRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    GrupoEntidade entity = findAtivo(scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId(), grupoId);
    String nome = normalizeNome(request.nome());
    String nomeNorm = normalizeNomeKey(nome);

    GrupoEntidade newParent = resolveParent(scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId(), request.parentId());
    Long newParentId = newParent == null ? null : newParent.getId();
    if (newParentId != null && newParentId.equals(entity.getId())) {
      throw new IllegalArgumentException("grupo_entidade_ciclo_invalido");
    }
    if (newParent != null && newParent.getPath().startsWith(entity.getPath() + "/")) {
      throw new IllegalArgumentException("grupo_entidade_ciclo_invalido");
    }

    validateUniqueName(scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId(), newParentId, nomeNorm, entity.getId());

    String oldPath = entity.getPath();
    int oldNivel = entity.getNivel();
    int newNivel = newParent == null ? 0 : newParent.getNivel() + 1;
    String newPath = newParent == null
      ? formatNode(entity.getId())
      : newParent.getPath() + "/" + formatNode(entity.getId());

    entity.setNome(nome);
    entity.setNomeNormalizado(nomeNorm);
    entity.setParentId(newParentId);
    entity.setNivel(newNivel);
    entity.setPath(newPath);
    if (request.ordem() != null && request.ordem() > 0) {
      entity.setOrdem(request.ordem());
    }
    repository.save(entity);

    if (!oldPath.equals(newPath) || oldNivel != newNivel) {
      List<GrupoEntidade> subtree = repository
        .findAllByTenantIdAndTipoEntidadeConfigAgrupadorIdAndPathStartingWithAndAtivoTrueOrderByPathAsc(
          scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId(), oldPath + "/");
      for (GrupoEntidade child : subtree) {
        String suffix = child.getPath().substring((oldPath + "/").length());
        child.setPath(newPath + "/" + suffix);
        child.setNivel(child.getNivel() - oldNivel + newNivel);
      }
      repository.saveAll(subtree);
    }

    auditService.log(scope.tenantId(), "GRUPO_ENTIDADE_ATUALIZADO", "grupo_entidade", String.valueOf(entity.getId()),
      "tipoEntidadeId=" + tipoEntidadeId + ";configAgrupadorId=" + scope.tipoEntidadeConfigAgrupadorId()
        + ";nome=" + entity.getNome() + ";parentId=" + String.valueOf(entity.getParentId()));
    return toResponse(entity);
  }

  @Transactional
  public void delete(Long tipoEntidadeId, Long grupoId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    GrupoEntidade entity = findAtivo(scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId(), grupoId);
    String pathPrefix = entity.getPath();
    List<GrupoEntidade> subtree = new ArrayList<>();
    subtree.add(entity);
    subtree.addAll(repository.findAllByTenantIdAndTipoEntidadeConfigAgrupadorIdAndPathStartingWithAndAtivoTrueOrderByPathAsc(
      scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId(), pathPrefix + "/"));

    Set<Long> ids = subtree.stream().map(GrupoEntidade::getId).collect(Collectors.toSet());
    if (registroRepository.existsByTenantIdAndTipoEntidadeConfigAgrupadorIdAndGrupoEntidadeIdInAndAtivoTrue(
        scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId(), ids)) {
      throw new IllegalArgumentException("grupo_entidade_possui_entidades");
    }

    subtree.forEach(group -> group.setAtivo(false));
    repository.saveAll(subtree);

    auditService.log(scope.tenantId(), "GRUPO_ENTIDADE_EXCLUIDO", "grupo_entidade", String.valueOf(entity.getId()),
      "tipoEntidadeId=" + tipoEntidadeId + ";configAgrupadorId=" + scope.tipoEntidadeConfigAgrupadorId()
        + ";nome=" + entity.getNome());
  }

  private List<GrupoEntidadeResponse> toTree(List<GrupoEntidade> groups, Map<Long, Long> directCountByGroupId) {
    Map<Long, GrupoEntidadeResponse> map = new LinkedHashMap<>();
    List<GrupoEntidadeResponse> roots = new ArrayList<>();
    for (GrupoEntidade entity : groups) {
      map.put(entity.getId(), toResponse(entity, directCountByGroupId.getOrDefault(entity.getId(), 0L)));
    }
    for (GrupoEntidade entity : groups) {
      GrupoEntidadeResponse node = map.get(entity.getId());
      if (entity.getParentId() == null) {
        roots.add(node);
        continue;
      }
      GrupoEntidadeResponse parent = map.get(entity.getParentId());
      if (parent == null) {
        roots.add(node);
      } else {
        parent.getChildren().add(node);
      }
    }
    for (GrupoEntidadeResponse root : roots) {
      sumTreeCount(root);
    }
    return roots;
  }

  private GrupoEntidadeResponse toResponse(GrupoEntidade entity) {
    return toResponse(entity, 0L);
  }

  private GrupoEntidadeResponse toResponse(GrupoEntidade entity, Long directCount) {
    return new GrupoEntidadeResponse(
      entity.getId(),
      entity.getNome(),
      entity.getParentId(),
      entity.getNivel(),
      entity.getOrdem(),
      entity.getPath(),
      entity.isAtivo(),
      directCount == null ? 0L : directCount);
  }

  private long sumTreeCount(GrupoEntidadeResponse node) {
    long total = node.getTotalRegistros() == null ? 0L : node.getTotalRegistros();
    for (GrupoEntidadeResponse child : node.getChildren()) {
      total += sumTreeCount(child);
    }
    node.setTotalRegistros(total);
    return total;
  }

  private Map<Long, Long> loadDirectCountByGroupId(Long tenantId, Long tipoEntidadeConfigAgrupadorId) {
    Map<Long, Long> map = new HashMap<>();
    for (RegistroEntidadeRepository.GrupoEntidadeCountRow row :
        registroRepository.countAtivosByGrupo(tenantId, tipoEntidadeConfigAgrupadorId)) {
      if (row == null || row.getGrupoId() == null) {
        continue;
      }
      map.put(row.getGrupoId(), row.getTotal() == null ? 0L : row.getTotal());
    }
    return map;
  }

  private GrupoEntidade resolveParent(Long tenantId, Long configAgrupadorId, Long parentId) {
    if (parentId == null) return null;
    return findAtivo(tenantId, configAgrupadorId, parentId);
  }

  private GrupoEntidade findAtivo(Long tenantId, Long configAgrupadorId, Long id) {
    return repository.findByIdAndTenantIdAndTipoEntidadeConfigAgrupadorIdAndAtivoTrue(id, tenantId, configAgrupadorId)
      .orElseThrow(() -> new EntityNotFoundException("grupo_entidade_not_found"));
  }

  private void validateUniqueName(
      Long tenantId,
      Long configAgrupadorId,
      Long parentId,
      String nomeNormalizado,
      Long currentId) {
    boolean duplicated;
    if (currentId == null) {
      duplicated = repository.existsByTenantIdAndTipoEntidadeConfigAgrupadorIdAndParentIdAndNomeNormalizadoAndAtivoTrue(
        tenantId, configAgrupadorId, parentId, nomeNormalizado);
    } else {
      duplicated = repository.existsByTenantIdAndTipoEntidadeConfigAgrupadorIdAndParentIdAndNomeNormalizadoAndAtivoTrueAndIdNot(
        tenantId, configAgrupadorId, parentId, nomeNormalizado, currentId);
    }
    if (duplicated) {
      throw new IllegalArgumentException("grupo_entidade_nome_duplicado_mesmo_pai");
    }
  }

  private String normalizeNome(String nome) {
    if (nome == null || nome.isBlank()) {
      throw new IllegalArgumentException("grupo_entidade_nome_required");
    }
    String value = nome.trim();
    if (value.length() > 120) {
      throw new IllegalArgumentException("grupo_entidade_nome_too_long");
    }
    return value;
  }

  private String normalizeNomeKey(String nome) {
    return nome.trim().toLowerCase(Locale.ROOT);
  }

  private String formatNode(Long id) {
    return String.format("%08d", id);
  }
}
