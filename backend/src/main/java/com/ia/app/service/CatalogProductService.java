package com.ia.app.service;

import com.ia.app.domain.CatalogConfigurationType;
import com.ia.app.dto.CatalogItemPricePreviewRequest;
import com.ia.app.dto.CatalogItemPriceResponse;
import com.ia.app.dto.CatalogItemRequest;
import com.ia.app.dto.CatalogItemResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CatalogProductService {

  private final CatalogItemCrudSupportService supportService;

  public CatalogProductService(CatalogItemCrudSupportService supportService) {
    this.supportService = supportService;
  }

  public Page<CatalogItemResponse> list(
      Long codigo,
      String text,
      Long catalogGroupId,
      Boolean includeChildren,
      Boolean ativo,
      Pageable pageable) {
    return supportService.list(CatalogConfigurationType.PRODUCTS, codigo, text, catalogGroupId, includeChildren, ativo, pageable);
  }

  public CatalogItemResponse get(Long id) {
    return supportService.get(CatalogConfigurationType.PRODUCTS, id);
  }

  public List<CatalogItemPriceResponse> previewPrices(CatalogItemPricePreviewRequest request) {
    return supportService.previewPrices(CatalogConfigurationType.PRODUCTS, request);
  }

  public CatalogItemResponse create(CatalogItemRequest request) {
    return supportService.create(CatalogConfigurationType.PRODUCTS, request);
  }

  public CatalogItemResponse update(Long id, CatalogItemRequest request) {
    return supportService.update(CatalogConfigurationType.PRODUCTS, id, request);
  }

  public void delete(Long id) {
    supportService.delete(CatalogConfigurationType.PRODUCTS, id);
  }
}
