-- ===== BEGIN V10__catalog_stock_balance.sql =====

CREATE TABLE IF NOT EXISTS catalog_stock_balance (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalogo_id BIGINT NOT NULL,
  catalog_type VARCHAR(20) NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  agrupador_empresa_id BIGINT NOT NULL,
  estoque_tipo_id BIGINT NOT NULL,
  filial_id BIGINT NOT NULL,
  quantidade_atual NUMERIC(19,6) NOT NULL DEFAULT 0,
  preco_atual NUMERIC(19,6) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_catalog_stock_balance_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_catalog_stock_balance_agrupador_tenant
    FOREIGN KEY (agrupador_empresa_id, tenant_id)
    REFERENCES agrupador_empresa (id, tenant_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_catalog_stock_balance_stock_type
    FOREIGN KEY (estoque_tipo_id)
    REFERENCES catalog_stock_type (id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_catalog_stock_balance_filial
    FOREIGN KEY (filial_id)
    REFERENCES empresa (id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_stock_balance_scope
  ON catalog_stock_balance (tenant_id, catalog_type, catalogo_id, agrupador_empresa_id, estoque_tipo_id, filial_id);

CREATE INDEX IF NOT EXISTS idx_catalog_stock_balance_catalog
  ON catalog_stock_balance (tenant_id, catalog_type, catalogo_id);

CREATE INDEX IF NOT EXISTS idx_catalog_stock_balance_group_stock
  ON catalog_stock_balance (tenant_id, catalog_configuration_id, agrupador_empresa_id, estoque_tipo_id);

-- ===== END V10__catalog_stock_balance.sql =====
