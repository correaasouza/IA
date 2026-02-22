package com.ia.app.service;

import com.ia.app.domain.PriceBook;
import com.ia.app.dto.PriceBookRequest;
import com.ia.app.dto.PriceBookResponse;
import com.ia.app.repository.PriceBookRepository;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceBookService {

  private final PriceBookRepository repository;

  public PriceBookService(PriceBookRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public List<PriceBookResponse> list() {
    Long tenantId = requireTenant();
    ensureDefaultBook(tenantId);
    return repository.findAllByTenantIdOrderByNameAsc(tenantId).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public PriceBookResponse get(Long id) {
    Long tenantId = requireTenant();
    PriceBook entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("price_book_not_found"));
    return toResponse(entity);
  }

  @Transactional
  public PriceBookResponse create(PriceBookRequest request) {
    Long tenantId = requireTenant();
    String name = normalizeName(request.name());

    repository.findByTenantIdAndNameIgnoreCase(tenantId, name).ifPresent(existing -> {
      throw new IllegalArgumentException("price_book_name_duplicated");
    });

    PriceBook entity = new PriceBook();
    entity.setTenantId(tenantId);
    entity.setName(name);
    entity.setActive(Boolean.TRUE.equals(request.active()));

    boolean makeDefault = Boolean.TRUE.equals(request.defaultBook());
    if (makeDefault) {
      unsetDefaultForTenant(tenantId);
      entity.setDefaultBook(true);
    }

    PriceBook saved = save(entity);
    if (!makeDefault && repository.findByTenantIdAndDefaultBookTrue(tenantId).isEmpty()) {
      saved.setDefaultBook(true);
      saved = save(saved);
    }
    return toResponse(saved);
  }

  @Transactional
  public PriceBookResponse update(Long id, PriceBookRequest request) {
    Long tenantId = requireTenant();
    PriceBook entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("price_book_not_found"));

    String name = normalizeName(request.name());
    repository.findByTenantIdAndNameIgnoreCase(tenantId, name).ifPresent(existing -> {
      if (!existing.getId().equals(entity.getId())) {
        throw new IllegalArgumentException("price_book_name_duplicated");
      }
    });

    entity.setName(name);
    entity.setActive(Boolean.TRUE.equals(request.active()));

    boolean makeDefault = Boolean.TRUE.equals(request.defaultBook());
    if (makeDefault) {
      unsetDefaultForTenant(tenantId);
      entity.setDefaultBook(true);
    } else if (entity.isDefaultBook()) {
      entity.setDefaultBook(false);
    }

    PriceBook saved = save(entity);
    if (repository.findByTenantIdAndDefaultBookTrue(tenantId).isEmpty()) {
      saved.setDefaultBook(true);
      saved = save(saved);
    }
    return toResponse(saved);
  }

  @Transactional
  public void delete(Long id) {
    Long tenantId = requireTenant();
    PriceBook entity = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("price_book_not_found"));
    repository.delete(entity);
  }

  @Transactional
  public PriceBook ensureDefaultBook(Long tenantId) {
    return repository.findByTenantIdAndDefaultBookTrue(tenantId)
      .orElseGet(() -> {
        PriceBook entity = new PriceBook();
        entity.setTenantId(tenantId);
        entity.setName(nextDefaultName(tenantId));
        entity.setActive(true);
        entity.setDefaultBook(true);
        return save(entity);
      });
  }

  @Transactional(readOnly = true)
  public boolean exists(Long tenantId, Long id) {
    if (tenantId == null || id == null) {
      return false;
    }
    return repository.existsByTenantIdAndId(tenantId, id);
  }

  private void unsetDefaultForTenant(Long tenantId) {
    List<PriceBook> rows = repository.findAllByTenantIdOrderByNameAsc(tenantId);
    for (PriceBook item : rows) {
      if (item.isDefaultBook()) {
        item.setDefaultBook(false);
        repository.save(item);
      }
    }
  }

  private String nextDefaultName(Long tenantId) {
    String base = "Padrao";
    if (repository.findByTenantIdAndNameIgnoreCase(tenantId, base).isEmpty()) {
      return base;
    }
    int suffix = 2;
    while (suffix < 1000) {
      String candidate = base + " " + suffix;
      if (repository.findByTenantIdAndNameIgnoreCase(tenantId, candidate).isEmpty()) {
        return candidate;
      }
      suffix++;
    }
    throw new IllegalStateException("price_book_default_generation_failed");
  }

  private PriceBook save(PriceBook entity) {
    try {
      return repository.save(entity);
    } catch (DataIntegrityViolationException ex) {
      String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage().toLowerCase();
      if (message.contains("ux_price_book_tenant_name")) {
        throw new IllegalArgumentException("price_book_name_duplicated");
      }
      throw ex;
    }
  }

  private String normalizeName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("price_book_name_required");
    }
    String normalized = name.trim();
    if (normalized.length() > 120) {
      throw new IllegalArgumentException("price_book_name_too_long");
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

  private PriceBookResponse toResponse(PriceBook entity) {
    return new PriceBookResponse(
      entity.getId(),
      entity.getName(),
      entity.isActive(),
      entity.isDefaultBook());
  }
}
