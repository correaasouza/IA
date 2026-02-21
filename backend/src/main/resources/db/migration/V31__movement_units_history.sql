ALTER TABLE movimento_estoque_item
  ADD COLUMN IF NOT EXISTS tenant_unit_id UUID,
  ADD COLUMN IF NOT EXISTS unidade_base_catalogo_tenant_unit_id UUID,
  ADD COLUMN IF NOT EXISTS quantidade_convertida_base NUMERIC(19,6),
  ADD COLUMN IF NOT EXISTS fator_aplicado NUMERIC(24,12),
  ADD COLUMN IF NOT EXISTS fator_fonte VARCHAR(40);

ALTER TABLE movimento_estoque_item
  ADD CONSTRAINT ck_mov_estoque_item_qtd_convertida_non_negative
    CHECK (quantidade_convertida_base IS NULL OR quantidade_convertida_base >= 0);

ALTER TABLE movimento_estoque_item
  ADD CONSTRAINT ck_mov_estoque_item_fator_positive
    CHECK (fator_aplicado IS NULL OR fator_aplicado > 0);

ALTER TABLE movimento_estoque_item
  ADD CONSTRAINT fk_mov_estoque_item_unit_informed
    FOREIGN KEY (tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE RESTRICT;

ALTER TABLE movimento_estoque_item
  ADD CONSTRAINT fk_mov_estoque_item_unit_base_catalog
    FOREIGN KEY (unidade_base_catalogo_tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_mov_estoque_item_tenant_unit
  ON movimento_estoque_item (tenant_id, tenant_unit_id);

CREATE INDEX IF NOT EXISTS idx_mov_estoque_item_base_unit
  ON movimento_estoque_item (tenant_id, unidade_base_catalogo_tenant_unit_id);

ALTER TABLE catalog_movement
  ADD COLUMN IF NOT EXISTS tenant_unit_id UUID,
  ADD COLUMN IF NOT EXISTS unidade_base_catalogo_tenant_unit_id UUID,
  ADD COLUMN IF NOT EXISTS quantidade_informada NUMERIC(19,6),
  ADD COLUMN IF NOT EXISTS quantidade_convertida_base NUMERIC(19,6),
  ADD COLUMN IF NOT EXISTS fator_aplicado NUMERIC(24,12),
  ADD COLUMN IF NOT EXISTS fator_fonte VARCHAR(40);

ALTER TABLE catalog_movement
  ADD CONSTRAINT ck_catalog_movement_qtd_informada_non_negative
    CHECK (quantidade_informada IS NULL OR quantidade_informada >= 0);

ALTER TABLE catalog_movement
  ADD CONSTRAINT ck_catalog_movement_qtd_convertida_non_negative
    CHECK (quantidade_convertida_base IS NULL OR quantidade_convertida_base >= 0);

ALTER TABLE catalog_movement
  ADD CONSTRAINT ck_catalog_movement_fator_positive
    CHECK (fator_aplicado IS NULL OR fator_aplicado > 0);

ALTER TABLE catalog_movement
  ADD CONSTRAINT fk_catalog_movement_unit_informed
    FOREIGN KEY (tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE RESTRICT;

ALTER TABLE catalog_movement
  ADD CONSTRAINT fk_catalog_movement_unit_base_catalog
    FOREIGN KEY (unidade_base_catalogo_tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_catalog_movement_tenant_unit
  ON catalog_movement (tenant_id, tenant_unit_id);

CREATE INDEX IF NOT EXISTS idx_catalog_movement_base_unit
  ON catalog_movement (tenant_id, unidade_base_catalogo_tenant_unit_id);
