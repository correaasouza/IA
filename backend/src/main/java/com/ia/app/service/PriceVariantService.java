package com.ia.app.service;

import com.ia.app.domain.PriceVariant;
import com.ia.app.dto.PriceVariantRequest;
import com.ia.app.dto.PriceVariantResponse;
import com.ia.app.repository.PriceVariantRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceVariantService {

  private final PriceVariantRepository repository;

  public PriceVariantService(PriceVariantRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<PriceVariantResponse> list() {
    Long tenantId = requireTenant();
    return repository.findAllByTenantIdOrderByNameAsc(tenantId).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public PriceVariantResponse get(Long id) {
    Long tenantId = requireTenant();
    PriceVariant entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("price_variant_not_found"));
    return toResponse(entity);
  }

  @Transactional
  public PriceVariantResponse create(PriceVariantRequest request) {
    Long tenantId = requireTenant();
    String name = normalizeName(request.name());

    repository.findByTenantIdAndNameIgnoreCase(tenantId, name).ifPresent(existing -> {
      throw new IllegalArgumentException("price_variant_name_duplicated");
    });

    PriceVariant entity = new PriceVariant();
    entity.setTenantId(tenantId);
    entity.setName(name);
    entity.setActive(Boolean.TRUE.equals(request.active()));
    return toResponse(save(entity));
  }

  @Transactional
  public PriceVariantResponse update(Long id, PriceVariantRequest request) {
    Long tenantId = requireTenant();
    PriceVariant entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("price_variant_not_found"));

    String name = normalizeName(request.name());
    repository.findByTenantIdAndNameIgnoreCase(tenantId, name).ifPresent(existing -> {
      if (!existing.getId().equals(entity.getId())) {
        throw new IllegalArgumentException("price_variant_name_duplicated");
      }
    });

    entity.setName(name);
    entity.setActive(Boolean.TRUE.equals(request.active()));
    return toResponse(save(entity));
  }

  @Transactional
  public void delete(Long id) {
    Long tenantId = requireTenant();
    PriceVariant entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("price_variant_not_found"));
    repository.delete(entity);
  }

  @Transactional(readOnly = true)
  public PriceVariant findById(Long tenantId, Long id) {
    if (id == null) {
      return null;
    }
    return repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("price_variant_not_found"));
  }

  private PriceVariant save(PriceVariant entity) {
    try {
      return repository.save(entity);
    } catch (DataIntegrityViolationException ex) {
      String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage().toLowerCase();
      if (message.contains("ux_price_variant_tenant_name")) {
        throw new IllegalArgumentException("price_variant_name_duplicated");
      }
      throw ex;
    }
  }

  private String normalizeName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("price_variant_name_required");
    }
    String normalized = name.trim();
    if (normalized.length() > 120) {
      throw new IllegalArgumentException("price_variant_name_too_long");
    }
    return normalized;
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }

  private PriceVariantResponse toResponse(PriceVariant entity) {
    return new PriceVariantResponse(entity.getId(), entity.getName(), entity.isActive());
  }
}
