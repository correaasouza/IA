package com.ia.app.service;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogItemBase;
import com.ia.app.repository.CatalogMovementRepository;
import com.ia.app.repository.CatalogProductRepository;
import com.ia.app.repository.CatalogServiceItemRepository;
import com.ia.app.repository.MovimentoEstoqueItemRepository;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogUnitLockService {

  private final CatalogProductRepository productRepository;
  private final CatalogServiceItemRepository serviceItemRepository;
  private final CatalogMovementRepository catalogMovementRepository;
  private final MovimentoEstoqueItemRepository movimentoEstoqueItemRepository;

  public CatalogUnitLockService(
      CatalogProductRepository productRepository,
      CatalogServiceItemRepository serviceItemRepository,
      CatalogMovementRepository catalogMovementRepository,
      MovimentoEstoqueItemRepository movimentoEstoqueItemRepository) {
    this.productRepository = productRepository;
    this.serviceItemRepository = serviceItemRepository;
    this.catalogMovementRepository = catalogMovementRepository;
    this.movimentoEstoqueItemRepository = movimentoEstoqueItemRepository;
  }

  @Transactional
  public void enforceUpdateRules(
      CatalogConfigurationType type,
      CatalogItemBase current,
      UUID novoTenantUnitId,
      UUID novaUnidadeAlternativaId,
      BigDecimal novoFatorAlternativo) {
    boolean locked = hasStockMovements(type, current);
    if (!locked) {
      return;
    }

    if (!Objects.equals(current.getTenantUnitId(), novoTenantUnitId)) {
      throw new IllegalArgumentException("catalog_item_unit_locked_by_stock_movements");
    }
    if (!Objects.equals(current.getUnidadeAlternativaTenantUnitId(), novaUnidadeAlternativaId)) {
      throw new IllegalArgumentException("catalog_item_unit_locked_by_stock_movements");
    }
    BigDecimal currentFactor = normalize(current.getFatorConversaoAlternativa());
    BigDecimal nextFactor = normalize(novoFatorAlternativo);
    if (!Objects.equals(currentFactor, nextFactor)) {
      throw new IllegalArgumentException("catalog_item_unit_locked_by_stock_movements");
    }
  }

  @Transactional(readOnly = true)
  public boolean hasStockMovements(CatalogConfigurationType type, CatalogItemBase current) {
    if (current == null || current.getTenantId() == null || current.getId() == null) {
      return false;
    }
    if (current.isHasStockMovements()) {
      return true;
    }

    boolean existsInLedger = catalogMovementRepository.existsByTenantIdAndCatalogTypeAndCatalogoId(
      current.getTenantId(),
      type,
      current.getId());
    if (existsInLedger) {
      return true;
    }

    return movimentoEstoqueItemRepository.existsByTenantIdAndCatalogTypeAndCatalogItemIdAndEstoqueMovimentadoTrue(
      current.getTenantId(),
      type,
      current.getId());
  }

  @Transactional
  public void markHasStockMovements(Long tenantId, CatalogConfigurationType type, Long catalogItemId) {
    if (tenantId == null || tenantId <= 0 || catalogItemId == null || catalogItemId <= 0) {
      return;
    }
    if (type == CatalogConfigurationType.PRODUCTS) {
      productRepository.markHasStockMovements(tenantId, catalogItemId);
      return;
    }
    serviceItemRepository.markHasStockMovements(tenantId, catalogItemId);
  }

  private BigDecimal normalize(BigDecimal value) {
    if (value == null) {
      return null;
    }
    return value.stripTrailingZeros();
  }
}
