-- ===== BEGIN V11__catalog_stock_adjustment.sql =====

CREATE TABLE IF NOT EXISTS catalog_stock_adjustment (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  codigo VARCHAR(40) NOT NULL,
  nome VARCHAR(120) NOT NULL,
  ordem INTEGER NOT NULL DEFAULT 1,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_catalog_stock_adjustment_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_stock_adjustment_scope_codigo_active
  ON catalog_stock_adjustment (tenant_id, catalog_configuration_id, codigo, active);

CREATE INDEX IF NOT EXISTS idx_catalog_stock_adjustment_scope
  ON catalog_stock_adjustment (tenant_id, catalog_configuration_id, active, ordem);

-- ===== END V11__catalog_stock_adjustment.sql =====

