package com.ia.app.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
  name = "catalog_service_item",
  uniqueConstraints = {
    @UniqueConstraint(
      name = "ux_catalog_service_item_codigo_scope",
      columnNames = {"tenant_id", "catalog_configuration_id", "agrupador_empresa_id", "codigo"})
  })
public class CatalogServiceItem extends CatalogItemBase {
}
