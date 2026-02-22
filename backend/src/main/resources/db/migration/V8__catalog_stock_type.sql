-- ===== BEGIN V8__catalog_stock_type.sql =====

CREATE TABLE IF NOT EXISTS catalog_stock_type (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  agrupador_empresa_id BIGINT NOT NULL,
  codigo VARCHAR(40) NOT NULL,
  nome VARCHAR(120) NOT NULL,
  ordem INTEGER NOT NULL DEFAULT 1,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT fk_catalog_stock_type_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_catalog_stock_type_agrupador_tenant
    FOREIGN KEY (agrupador_empresa_id, tenant_id)
    REFERENCES agrupador_empresa (id, tenant_id)
    ON DELETE RESTRICT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_stock_type_scope_codigo_active
  ON catalog_stock_type (tenant_id, catalog_configuration_id, agrupador_empresa_id, codigo, active);

CREATE INDEX IF NOT EXISTS idx_catalog_stock_type_scope
  ON catalog_stock_type (tenant_id, catalog_configuration_id, agrupador_empresa_id, active, ordem);

-- ===== END V8__catalog_stock_type.sql =====
