package com.ia.app.service;

import com.ia.app.domain.OfficialUnit;
import com.ia.app.domain.OfficialUnitOrigin;
import com.ia.app.dto.OfficialUnitRequest;
import com.ia.app.dto.OfficialUnitResponse;
import com.ia.app.repository.OfficialUnitRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficialUnitService {

  private final OfficialUnitRepository officialUnitRepository;
  private final TenantUnitMirrorService tenantUnitMirrorService;

  public OfficialUnitService(
      OfficialUnitRepository officialUnitRepository,
      TenantUnitMirrorService tenantUnitMirrorService) {
    this.officialUnitRepository = officialUnitRepository;
    this.tenantUnitMirrorService = tenantUnitMirrorService;
  }

  @Transactional(readOnly = true)
  public List<OfficialUnitResponse> list(Boolean ativo, String text) {
    String normalizedText = normalizeOptional(text);
    return officialUnitRepository.findAll().stream()
      .filter(unit -> ativo == null || unit.isAtivo() == ativo)
      .filter(unit -> {
        if (normalizedText == null) {
          return true;
        }
        String code = unit.getCodigoOficial() == null ? "" : unit.getCodigoOficial().toLowerCase(Locale.ROOT);
        String description = unit.getDescricao() == null ? "" : unit.getDescricao().toLowerCase(Locale.ROOT);
        return code.contains(normalizedText) || description.contains(normalizedText);
      })
      .sorted(Comparator.comparing(OfficialUnit::getCodigoOficial, String.CASE_INSENSITIVE_ORDER))
      .map(this::toResponse)
      .toList();
  }

  @Transactional(readOnly = true)
  public OfficialUnitResponse get(UUID id) {
    return toResponse(findById(id));
  }

  @Transactional
  public OfficialUnitResponse create(OfficialUnitRequest request) {
    String code = normalizeCode(request.codigoOficial());
    if (officialUnitRepository.existsByCodigoOficialIgnoreCase(code)) {
      throw new IllegalArgumentException("official_unit_codigo_duplicado");
    }

    OfficialUnit entity = new OfficialUnit();
    entity.setCodigoOficial(code);
    entity.setDescricao(normalizeDescription(request.descricao(), code));
    entity.setAtivo(Boolean.TRUE.equals(request.ativo()));
    entity.setOrigem(request.origem() == null ? OfficialUnitOrigin.MANUAL : request.origem());

    OfficialUnit saved = officialUnitRepository.save(entity);
    tenantUnitMirrorService.reconcileAllTenants();
    return toResponse(saved);
  }

  @Transactional
  public OfficialUnitResponse update(UUID id, OfficialUnitRequest request) {
    OfficialUnit entity = findById(id);
    String requestedCode = normalizeCode(request.codigoOficial());
    if (!requestedCode.equalsIgnoreCase(entity.getCodigoOficial())) {
      throw new IllegalArgumentException("official_unit_codigo_immutable");
    }

    entity.setDescricao(normalizeDescription(request.descricao(), requestedCode));
    entity.setAtivo(Boolean.TRUE.equals(request.ativo()));
    if (request.origem() != null) {
      entity.setOrigem(request.origem());
    }

    OfficialUnit saved = officialUnitRepository.save(entity);
    tenantUnitMirrorService.reconcileAllTenants();
    return toResponse(saved);
  }

  @Transactional
  public void delete(UUID id) {
    OfficialUnit entity = findById(id);
    try {
      officialUnitRepository.delete(entity);
      officialUnitRepository.flush();
    } catch (DataIntegrityViolationException ex) {
      throw new IllegalArgumentException("official_unit_in_use");
    }
  }

  private OfficialUnit findById(UUID id) {
    if (id == null) {
      throw new IllegalArgumentException("official_unit_id_invalid");
    }
    return officialUnitRepository.findById(id)
      .orElseThrow(() -> new EntityNotFoundException("official_unit_not_found"));
  }

  private OfficialUnitResponse toResponse(OfficialUnit unit) {
    return new OfficialUnitResponse(
      unit.getId(),
      unit.getCodigoOficial(),
      unit.getDescricao(),
      unit.isAtivo(),
      unit.getOrigem());
  }

  private String normalizeCode(String value) {
    String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("official_unit_codigo_required");
    }
    if (normalized.length() > 20) {
      normalized = normalized.substring(0, 20);
    }
    return normalized;
  }

  private String normalizeDescription(String value, String fallbackCode) {
    String normalized = value == null ? "" : value.trim();
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("official_unit_descricao_required");
    }
    if (normalized.length() > 160) {
      normalized = normalized.substring(0, 160);
    }
    return normalized;
  }

  private String normalizeOptional(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim().toLowerCase(Locale.ROOT);
  }
}
