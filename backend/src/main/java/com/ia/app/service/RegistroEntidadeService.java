package com.ia.app.service;

import com.ia.app.domain.GrupoEntidade;
import com.ia.app.domain.Pessoa;
import com.ia.app.domain.RegistroEntidade;
import com.ia.app.dto.PessoaVinculoResponse;
import com.ia.app.dto.RegistroEntidadeRequest;
import com.ia.app.dto.RegistroEntidadeResponse;
import com.ia.app.repository.GrupoEntidadeRepository;
import com.ia.app.repository.PessoaRepository;
import com.ia.app.repository.PriceBookRepository;
import com.ia.app.repository.RegistroEntidadeRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroEntidadeService {

  private final RegistroEntidadeRepository repository;
  private final RegistroEntidadeContextoService contextoService;
  private final RegistroEntidadeCodigoService codigoService;
  private final PessoaResolveService pessoaResolveService;
  private final PessoaRepository pessoaRepository;
  private final GrupoEntidadeRepository grupoRepository;
  private final PriceBookRepository priceBookRepository;
  private final AuditService auditService;

  public RegistroEntidadeService(
      RegistroEntidadeRepository repository,
      RegistroEntidadeContextoService contextoService,
      RegistroEntidadeCodigoService codigoService,
      PessoaResolveService pessoaResolveService,
      PessoaRepository pessoaRepository,
      GrupoEntidadeRepository grupoRepository,
      PriceBookRepository priceBookRepository,
      AuditService auditService) {
    this.repository = repository;
    this.contextoService = contextoService;
    this.codigoService = codigoService;
    this.pessoaResolveService = pessoaResolveService;
    this.pessoaRepository = pessoaRepository;
    this.grupoRepository = grupoRepository;
    this.priceBookRepository = priceBookRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public Page<RegistroEntidadeResponse> list(
      Long tipoEntidadeId,
      Long codigo,
      String pessoaNome,
      String registroFederal,
      Long grupoId,
      Boolean ativo,
      Pageable pageable) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    Page<RegistroEntidade> page = repository.search(
      scope.tenantId(),
      scope.tipoEntidadeConfigAgrupadorId(),
      codigo,
      normalizeSearch(pessoaNome),
      normalizeRegistroFilter(registroFederal),
      grupoId,
      ativo,
      pageable);
    return page.map(item -> toResponse(scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId(), item));
  }

  @Transactional(readOnly = true)
  public RegistroEntidadeResponse get(Long tipoEntidadeId, Long id) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade entity = repository
      .findByIdAndTenantIdAndTipoEntidadeConfigAgrupadorId(
        id, scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId())
      .orElseThrow(() -> new EntityNotFoundException("registro_entidade_not_found"));
    return toResponse(scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId(), entity);
  }

  @Transactional
  public RegistroEntidadeResponse create(Long tipoEntidadeId, RegistroEntidadeRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    Long grupoId = validateGrupo(scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId(), request.grupoEntidadeId());
    Long priceBookId = validatePriceBook(scope.tenantId(), request.priceBookId());
    Pessoa pessoa = pessoaResolveService.resolveOrCreate(scope.tenantId(), request.pessoa());

    RegistroEntidade entity = new RegistroEntidade();
    entity.setTenantId(scope.tenantId());
    entity.setTipoEntidadeConfigAgrupadorId(scope.tipoEntidadeConfigAgrupadorId());
    entity.setCodigo(codigoService.proximoCodigo(scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId()));
    entity.setPessoaId(pessoa.getId());
    entity.setGrupoEntidadeId(grupoId);
    entity.setPriceBookId(priceBookId);
    entity.setAtivo(Boolean.TRUE.equals(request.ativo()));

    RegistroEntidade saved = saveWithIntegrityMap(entity);
    auditService.log(scope.tenantId(), "REGISTRO_ENTIDADE_CRIADO", "registro_entidade", String.valueOf(saved.getId()),
      "tipoEntidadeId=" + tipoEntidadeId + ";configAgrupadorId=" + scope.tipoEntidadeConfigAgrupadorId()
        + ";codigo=" + saved.getCodigo() + ";pessoaId=" + saved.getPessoaId());
    return toResponse(scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId(), saved);
  }

  @Transactional
  public RegistroEntidadeResponse update(Long tipoEntidadeId, Long id, RegistroEntidadeRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade entity = repository
      .findByIdAndTenantIdAndTipoEntidadeConfigAgrupadorId(
        id, scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId())
      .orElseThrow(() -> new EntityNotFoundException("registro_entidade_not_found"));

    Long grupoId = validateGrupo(scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId(), request.grupoEntidadeId());
    Long priceBookId = validatePriceBook(scope.tenantId(), request.priceBookId());
    Pessoa pessoa = pessoaResolveService.resolveOrCreate(scope.tenantId(), request.pessoa());

    entity.setGrupoEntidadeId(grupoId);
    entity.setPriceBookId(priceBookId);
    entity.setPessoaId(pessoa.getId());
    entity.setAtivo(Boolean.TRUE.equals(request.ativo()));

    RegistroEntidade saved = saveWithIntegrityMap(entity);
    auditService.log(scope.tenantId(), "REGISTRO_ENTIDADE_ATUALIZADO", "registro_entidade", String.valueOf(saved.getId()),
      "tipoEntidadeId=" + tipoEntidadeId + ";configAgrupadorId=" + scope.tipoEntidadeConfigAgrupadorId()
        + ";codigo=" + saved.getCodigo() + ";pessoaId=" + saved.getPessoaId());
    return toResponse(scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId(), saved);
  }

  @Transactional
  public void delete(Long tipoEntidadeId, Long id) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade entity = repository
      .findByIdAndTenantIdAndTipoEntidadeConfigAgrupadorId(
        id, scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId())
      .orElseThrow(() -> new EntityNotFoundException("registro_entidade_not_found"));
    entity.setAtivo(false);
    repository.save(entity);
    auditService.log(scope.tenantId(), "REGISTRO_ENTIDADE_EXCLUIDO", "registro_entidade", String.valueOf(entity.getId()),
      "tipoEntidadeId=" + tipoEntidadeId + ";configAgrupadorId=" + scope.tipoEntidadeConfigAgrupadorId()
        + ";codigo=" + entity.getCodigo());
  }

  private Long validateGrupo(Long tenantId, Long configAgrupadorId, Long grupoId) {
    if (grupoId == null) return null;
    GrupoEntidade grupo = grupoRepository
      .findByIdAndTenantIdAndTipoEntidadeConfigAgrupadorIdAndAtivoTrue(grupoId, tenantId, configAgrupadorId)
      .orElseThrow(() -> new EntityNotFoundException("grupo_entidade_not_found"));
    return grupo.getId();
  }

  private Long validatePriceBook(Long tenantId, Long priceBookId) {
    if (priceBookId == null) return null;
    if (!priceBookRepository.existsByTenantIdAndId(tenantId, priceBookId)) {
      throw new EntityNotFoundException("price_book_not_found");
    }
    return priceBookId;
  }

  private RegistroEntidade saveWithIntegrityMap(RegistroEntidade entity) {
    try {
      return repository.save(entity);
    } catch (DataIntegrityViolationException ex) {
      String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage().toLowerCase();
      if (message.contains("ux_registro_entidade_codigo_scope")) {
        throw new IllegalArgumentException("entidade_codigo_duplicado_configuracao");
      }
      if (message.contains("ux_pessoa_tenant_tipo_registro_federal_norm")) {
        throw new IllegalArgumentException("pessoa_registro_federal_duplicado");
      }
      throw ex;
    }
  }

  private RegistroEntidadeResponse toResponse(Long tenantId, Long configAgrupadorId, RegistroEntidade entity) {
    Pessoa pessoa = pessoaRepository.findByIdAndTenantId(entity.getPessoaId(), tenantId)
      .orElseThrow(() -> new EntityNotFoundException("pessoa_not_found"));
    String grupoNome = null;
    if (entity.getGrupoEntidadeId() != null) {
      grupoNome = grupoRepository
        .findByIdAndTenantIdAndTipoEntidadeConfigAgrupadorIdAndAtivoTrue(
          entity.getGrupoEntidadeId(), tenantId, configAgrupadorId)
        .map(GrupoEntidade::getNome)
        .orElse(null);
    }
    return new RegistroEntidadeResponse(
      entity.getId(),
      entity.getTipoEntidadeConfigAgrupadorId(),
      entity.getCodigo(),
      entity.getGrupoEntidadeId(),
      grupoNome,
      entity.getPriceBookId(),
      entity.isAtivo(),
      new PessoaVinculoResponse(
        pessoa.getId(),
        pessoa.getNome(),
        pessoa.getApelido(),
        pessoa.getTipoRegistro(),
        pessoa.getRegistroFederal()));
  }

  @Transactional(readOnly = true)
  public Map<Long, String> grupoNomeById(Long tenantId, Long configAgrupadorId, Set<Long> grupoIds) {
    if (grupoIds == null || grupoIds.isEmpty()) return Map.of();
    return grupoRepository
      .findAllByTenantIdAndTipoEntidadeConfigAgrupadorIdAndIdIn(tenantId, configAgrupadorId, grupoIds)
      .stream()
      .collect(Collectors.toMap(GrupoEntidade::getId, GrupoEntidade::getNome, (a, b) -> a, HashMap::new));
  }

  private String normalizeSearch(String value) {
    if (value == null) return null;
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private String normalizeRegistroFilter(String value) {
    if (value == null || value.isBlank()) return null;
    String trimmed = value.trim();
    String digits = trimmed.replaceAll("\\D", "");
    if (digits.length() == 11 || digits.length() == 14) {
      return digits;
    }
    return trimmed.toUpperCase();
  }
}
