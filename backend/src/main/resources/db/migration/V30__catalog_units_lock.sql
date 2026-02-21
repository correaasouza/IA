ALTER TABLE catalog_product
  ADD COLUMN IF NOT EXISTS tenant_unit_id UUID,
  ADD COLUMN IF NOT EXISTS unidade_alternativa_tenant_unit_id UUID,
  ADD COLUMN IF NOT EXISTS fator_conversao_alternativa NUMERIC(24,12),
  ADD COLUMN IF NOT EXISTS has_stock_movements BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE catalog_service_item
  ADD COLUMN IF NOT EXISTS tenant_unit_id UUID,
  ADD COLUMN IF NOT EXISTS unidade_alternativa_tenant_unit_id UUID,
  ADD COLUMN IF NOT EXISTS fator_conversao_alternativa NUMERIC(24,12),
  ADD COLUMN IF NOT EXISTS has_stock_movements BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE catalog_product
  ADD CONSTRAINT ck_catalog_product_fator_alt_positive
    CHECK (fator_conversao_alternativa IS NULL OR fator_conversao_alternativa > 0);

ALTER TABLE catalog_service_item
  ADD CONSTRAINT ck_catalog_service_item_fator_alt_positive
    CHECK (fator_conversao_alternativa IS NULL OR fator_conversao_alternativa > 0);

ALTER TABLE catalog_product
  ADD CONSTRAINT ck_catalog_product_alt_unit_diff
    CHECK (
      unidade_alternativa_tenant_unit_id IS NULL
      OR tenant_unit_id IS NULL
      OR unidade_alternativa_tenant_unit_id <> tenant_unit_id
    );

ALTER TABLE catalog_service_item
  ADD CONSTRAINT ck_catalog_service_item_alt_unit_diff
    CHECK (
      unidade_alternativa_tenant_unit_id IS NULL
      OR tenant_unit_id IS NULL
      OR unidade_alternativa_tenant_unit_id <> tenant_unit_id
    );

ALTER TABLE catalog_product
  ADD CONSTRAINT fk_catalog_product_tenant_unit_base
    FOREIGN KEY (tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE RESTRICT;

ALTER TABLE catalog_product
  ADD CONSTRAINT fk_catalog_product_tenant_unit_alt
    FOREIGN KEY (unidade_alternativa_tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE RESTRICT;

ALTER TABLE catalog_service_item
  ADD CONSTRAINT fk_catalog_service_item_tenant_unit_base
    FOREIGN KEY (tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE RESTRICT;

ALTER TABLE catalog_service_item
  ADD CONSTRAINT fk_catalog_service_item_tenant_unit_alt
    FOREIGN KEY (unidade_alternativa_tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_catalog_product_tenant_unit
  ON catalog_product (tenant_id, tenant_unit_id);

CREATE INDEX IF NOT EXISTS idx_catalog_product_alt_tenant_unit
  ON catalog_product (tenant_id, unidade_alternativa_tenant_unit_id);

CREATE INDEX IF NOT EXISTS idx_catalog_product_has_stock_movements
  ON catalog_product (tenant_id, has_stock_movements);

CREATE INDEX IF NOT EXISTS idx_catalog_service_item_tenant_unit
  ON catalog_service_item (tenant_id, tenant_unit_id);

CREATE INDEX IF NOT EXISTS idx_catalog_service_item_alt_tenant_unit
  ON catalog_service_item (tenant_id, unidade_alternativa_tenant_unit_id);

CREATE INDEX IF NOT EXISTS idx_catalog_service_item_has_stock_movements
  ON catalog_service_item (tenant_id, has_stock_movements);
