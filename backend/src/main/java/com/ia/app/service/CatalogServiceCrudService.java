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
public class CatalogServiceCrudService {

  private final CatalogItemCrudSupportService supportService;

  public CatalogServiceCrudService(CatalogItemCrudSupportService supportService) {
    this.supportService = supportService;
  }

  public Page<CatalogItemResponse> list(
      Long codigo,
      String text,
      Long catalogGroupId,
      Boolean includeChildren,
      Boolean ativo,
      Pageable pageable) {
    return supportService.list(CatalogConfigurationType.SERVICES, codigo, text, catalogGroupId, includeChildren, ativo, pageable);
  }

  public CatalogItemResponse get(Long id) {
    return supportService.get(CatalogConfigurationType.SERVICES, id);
  }

  public List<CatalogItemPriceResponse> previewPrices(CatalogItemPricePreviewRequest request) {
    return supportService.previewPrices(CatalogConfigurationType.SERVICES, request);
  }

  public CatalogItemResponse create(CatalogItemRequest request) {
    return supportService.create(CatalogConfigurationType.SERVICES, request);
  }

  public CatalogItemResponse update(Long id, CatalogItemRequest request) {
    return supportService.update(CatalogConfigurationType.SERVICES, id, request);
  }

  public void delete(Long id) {
    supportService.delete(CatalogConfigurationType.SERVICES, id);
  }
}
