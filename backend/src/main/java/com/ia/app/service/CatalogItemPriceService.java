package com.ia.app.service;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.domain.CatalogItemPrice;
import com.ia.app.domain.CatalogPriceRuleByGroup;
import com.ia.app.domain.CatalogPriceType;
import com.ia.app.domain.PriceAdjustmentKind;
import com.ia.app.domain.PriceBaseMode;
import com.ia.app.domain.PriceUiLockMode;
import com.ia.app.dto.CatalogItemPriceInput;
import com.ia.app.dto.CatalogItemPriceResponse;
import com.ia.app.repository.CatalogItemPriceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogItemPriceService {

  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

  private final CatalogItemPriceRepository repository;
  private final CatalogPriceRuleService ruleService;

  public CatalogItemPriceService(
      CatalogItemPriceRepository repository,
      CatalogPriceRuleService ruleService) {
    this.repository = repository;
    this.ruleService = ruleService;
  }

  @Transactional
  public List<CatalogItemPriceResponse> getOrCreateForItem(
      Long tenantId,
      CatalogConfigurationType catalogType,
      Long catalogItemId,
      Long catalogConfigurationByGroupId,
      boolean refreshModeIv) {
    return processForItem(
      tenantId,
      catalogType,
      catalogItemId,
      catalogConfigurationByGroupId,
      List.of(),
      refreshModeIv,
      false,
      true);
  }

  @Transactional
  public List<CatalogItemPriceResponse> upsertForItem(
      Long tenantId,
      CatalogConfigurationType catalogType,
      Long catalogItemId,
      Long catalogConfigurationByGroupId,
      List<CatalogItemPriceInput> inputs) {
    return processForItem(
      tenantId,
      catalogType,
      catalogItemId,
      catalogConfigurationByGroupId,
      inputs == null ? List.of() : inputs,
      false,
      true,
      true);
  }

  @Transactional(readOnly = true)
  public List<CatalogItemPriceResponse> previewForItem(
      Long tenantId,
      CatalogConfigurationType catalogType,
      Long catalogItemId,
      Long catalogConfigurationByGroupId,
      List<CatalogItemPriceInput> inputs) {
    return processForItem(
      tenantId,
      catalogType,
      catalogItemId,
      catalogConfigurationByGroupId,
      inputs == null ? List.of() : inputs,
      false,
      true,
      false);
  }

  private List<CatalogItemPriceResponse> processForItem(
      Long tenantId,
      CatalogConfigurationType catalogType,
      Long catalogItemId,
      Long catalogConfigurationByGroupId,
      List<CatalogItemPriceInput> inputs,
      boolean refreshModeIv,
      boolean applyInputs,
      boolean persist) {

    Map<CatalogPriceType, CatalogPriceRuleByGroup> rules = ruleService.loadRuleMap(tenantId, catalogConfigurationByGroupId);
    List<CatalogPriceType> order = ruleService.topologicalOrder(rules);
    Map<CatalogPriceType, CatalogItemPrice> existing = catalogItemId == null
      ? new EnumMap<>(CatalogPriceType.class)
      : loadExisting(tenantId, catalogType, catalogItemId);
    Map<CatalogPriceType, CatalogItemPriceInput> inputByType = indexInputs(inputs);
    Map<CatalogPriceType, MutablePriceState> states = new EnumMap<>(CatalogPriceType.class);

    for (CatalogPriceType type : CatalogPriceType.values()) {
      CatalogItemPrice row = existing.get(type);
      CatalogPriceRuleByGroup rule = rules.get(type);
      if (rule == null) {
        throw new IllegalArgumentException("catalog_price_rule_missing_type");
      }
      states.put(type, rowToState(row, rule));
    }

    for (CatalogPriceType type : order) {
      CatalogPriceRuleByGroup rule = rules.get(type);
      MutablePriceState state = states.get(type);
      CatalogItemPriceInput input = inputByType.get(type);
      BigDecimal baseValue = resolveBaseValue(rule, states);

      switch (rule.getUiLockMode()) {
        case II -> applyModeII(state, input, applyInputs);
        case I -> applyModeI(state, input, baseValue, applyInputs);
        case III -> applyModeIII(state, input, baseValue, applyInputs);
        case IV -> applyModeIV(state, rule, baseValue, applyInputs || refreshModeIv);
        default -> throw new IllegalArgumentException("catalog_item_price_mode_invalid");
      }

      state.priceFinal = normalizePrice(state.priceFinal);
      state.adjustmentValue = normalizeScale(state.adjustmentValue);
      if (state.adjustmentKind == null) {
        state.adjustmentKind = PriceAdjustmentKind.FIXED;
      }
    }

    if (!persist) {
      return states.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> toResponse(entry.getKey(), entry.getValue()))
        .toList();
    }

    List<CatalogItemPrice> toSave = new ArrayList<>();
    for (CatalogPriceType type : CatalogPriceType.values()) {
      MutablePriceState state = states.get(type);
      CatalogItemPrice row = existing.get(type);
      if (row == null) {
        row = new CatalogItemPrice();
        row.setTenantId(tenantId);
        row.setCatalogType(catalogType);
        row.setCatalogItemId(catalogItemId);
        row.setPriceType(type);
      }
      row.setPriceFinal(state.priceFinal);
      row.setAdjustmentKind(state.adjustmentKind);
      row.setAdjustmentValue(state.adjustmentValue);
      toSave.add(row);
    }

    List<CatalogItemPrice> saved = repository.saveAll(toSave);
    return saved.stream()
      .sorted(Comparator.comparing(CatalogItemPrice::getPriceType))
      .map(this::toResponse)
      .toList();
  }

  private void applyModeII(MutablePriceState state, CatalogItemPriceInput input, boolean applyInputs) {
    if (applyInputs && input != null && input.priceFinal() != null) {
      state.priceFinal = input.priceFinal();
    }
  }

  private void applyModeI(MutablePriceState state, CatalogItemPriceInput input, BigDecimal baseValue, boolean applyInputs) {
    if (applyInputs && input != null) {
      if (input.adjustmentKind() != null) {
        state.adjustmentKind = input.adjustmentKind();
      }
      if (input.adjustmentValue() != null) {
        state.adjustmentValue = input.adjustmentValue();
      }
    }
    state.priceFinal = calculatePrice(baseValue, state.adjustmentKind, state.adjustmentValue);
  }

  private void applyModeIII(MutablePriceState state, CatalogItemPriceInput input, BigDecimal baseValue, boolean applyInputs) {
    if (!applyInputs || input == null) {
      return;
    }

    if (input.adjustmentKind() != null) {
      state.adjustmentKind = input.adjustmentKind();
    }

    if (input.lastEditedField() == com.ia.app.domain.CatalogPriceEditedField.PRICE) {
      BigDecimal targetPrice = input.priceFinal() == null ? state.priceFinal : input.priceFinal();
      state.priceFinal = targetPrice;
      state.adjustmentValue = deriveAdjustment(baseValue, targetPrice, state.adjustmentKind);
      return;
    }

    if (input.adjustmentValue() != null) {
      state.adjustmentValue = input.adjustmentValue();
    }
    state.priceFinal = calculatePrice(baseValue, state.adjustmentKind, state.adjustmentValue);
  }

  private void applyModeIV(MutablePriceState state, CatalogPriceRuleByGroup rule, BigDecimal baseValue, boolean recalculate) {
    // Mode IV is fully driven by the group rule; item-level adjustment must not override it.
    state.adjustmentKind = rule.getAdjustmentKindDefault() == null ? PriceAdjustmentKind.FIXED : rule.getAdjustmentKindDefault();
    state.adjustmentValue = normalizeScale(rule.getAdjustmentDefault());
    if (recalculate) {
      state.priceFinal = calculatePrice(baseValue, state.adjustmentKind, state.adjustmentValue);
    }
  }

  private BigDecimal resolveBaseValue(CatalogPriceRuleByGroup rule, Map<CatalogPriceType, MutablePriceState> states) {
    if (rule.getBaseMode() == PriceBaseMode.NONE) {
      return BigDecimal.ZERO;
    }
    CatalogPriceType baseType = rule.getBasePriceType();
    if (baseType == null) {
      throw new IllegalArgumentException("catalog_price_rule_base_required");
    }
    MutablePriceState base = states.get(baseType);
    return base == null || base.priceFinal == null ? BigDecimal.ZERO : base.priceFinal;
  }

  private BigDecimal calculatePrice(BigDecimal baseValue, PriceAdjustmentKind kind, BigDecimal adjustment) {
    BigDecimal base = baseValue == null ? BigDecimal.ZERO : baseValue;
    BigDecimal adj = adjustment == null ? BigDecimal.ZERO : adjustment;
    if (kind == PriceAdjustmentKind.PERCENT) {
      BigDecimal factor = BigDecimal.ONE.add(adj.divide(ONE_HUNDRED, CatalogPriceRuleService.PRICE_SCALE, RoundingMode.HALF_UP));
      return base.multiply(factor).setScale(CatalogPriceRuleService.PRICE_SCALE, RoundingMode.HALF_UP);
    }
    return base.add(adj).setScale(CatalogPriceRuleService.PRICE_SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal deriveAdjustment(BigDecimal baseValue, BigDecimal priceValue, PriceAdjustmentKind kind) {
    BigDecimal base = baseValue == null ? BigDecimal.ZERO : baseValue;
    BigDecimal price = priceValue == null ? BigDecimal.ZERO : priceValue;

    if (kind == PriceAdjustmentKind.PERCENT) {
      if (base.compareTo(BigDecimal.ZERO) == 0) {
        return BigDecimal.ZERO.setScale(CatalogPriceRuleService.PRICE_SCALE, RoundingMode.HALF_UP);
      }
      return price
        .divide(base, CatalogPriceRuleService.PRICE_SCALE, RoundingMode.HALF_UP)
        .subtract(BigDecimal.ONE)
        .multiply(ONE_HUNDRED)
        .setScale(CatalogPriceRuleService.PRICE_SCALE, RoundingMode.HALF_UP);
    }

    return price.subtract(base).setScale(CatalogPriceRuleService.PRICE_SCALE, RoundingMode.HALF_UP);
  }

  private Map<CatalogPriceType, CatalogItemPrice> loadExisting(
      Long tenantId,
      CatalogConfigurationType catalogType,
      Long catalogItemId) {
    Map<CatalogPriceType, CatalogItemPrice> map = new EnumMap<>(CatalogPriceType.class);
    for (CatalogItemPrice row : repository.findAllByTenantIdAndCatalogTypeAndCatalogItemIdOrderByPriceTypeAsc(
      tenantId,
      catalogType,
      catalogItemId)) {
      map.put(row.getPriceType(), row);
    }
    return map;
  }

  private Map<CatalogPriceType, CatalogItemPriceInput> indexInputs(List<CatalogItemPriceInput> inputs) {
    Map<CatalogPriceType, CatalogItemPriceInput> map = new HashMap<>();
    for (CatalogItemPriceInput input : inputs) {
      if (input == null || input.priceType() == null) {
        continue;
      }
      if (map.put(input.priceType(), input) != null) {
        throw new IllegalArgumentException("catalog_item_price_duplicated_type_input");
      }
    }
    return map;
  }

  private MutablePriceState rowToState(CatalogItemPrice row, CatalogPriceRuleByGroup rule) {
    MutablePriceState state = new MutablePriceState();
    if (row != null) {
      state.priceFinal = normalizePrice(row.getPriceFinal());
      state.adjustmentKind = row.getAdjustmentKind() == null
        ? (rule.getAdjustmentKindDefault() == null ? PriceAdjustmentKind.FIXED : rule.getAdjustmentKindDefault())
        : row.getAdjustmentKind();
      state.adjustmentValue = normalizeScale(row.getAdjustmentValue());
      return state;
    }

    state.adjustmentKind = rule.getAdjustmentKindDefault() == null ? PriceAdjustmentKind.FIXED : rule.getAdjustmentKindDefault();
    state.adjustmentValue = normalizeScale(rule.getAdjustmentDefault());
    state.priceFinal = BigDecimal.ZERO.setScale(CatalogPriceRuleService.PRICE_SCALE, RoundingMode.HALF_UP);
    return state;
  }

  private CatalogItemPriceResponse toResponse(CatalogItemPrice row) {
    return new CatalogItemPriceResponse(
      row.getPriceType(),
      normalizePrice(row.getPriceFinal()),
      row.getAdjustmentKind(),
      normalizeScale(row.getAdjustmentValue()));
  }

  private CatalogItemPriceResponse toResponse(CatalogPriceType type, MutablePriceState state) {
    return new CatalogItemPriceResponse(
      type,
      normalizePrice(state.priceFinal),
      state.adjustmentKind == null ? PriceAdjustmentKind.FIXED : state.adjustmentKind,
      normalizeScale(state.adjustmentValue));
  }

  private BigDecimal normalizeScale(BigDecimal value) {
    BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
    return resolved.setScale(CatalogPriceRuleService.PRICE_SCALE, RoundingMode.HALF_UP);
  }

  private BigDecimal normalizePrice(BigDecimal value) {
    BigDecimal resolved = normalizeScale(value);
    if (resolved.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("catalog_item_price_negative");
    }
    return resolved;
  }

  private static final class MutablePriceState {
    private BigDecimal priceFinal;
    private PriceAdjustmentKind adjustmentKind;
    private BigDecimal adjustmentValue;
  }
}
