-- ===== BEGIN V19__movimento_item_tipo.sql =====

CREATE TABLE IF NOT EXISTS movimento_item_tipo (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  nome VARCHAR(120) NOT NULL,
  catalog_type VARCHAR(20) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_movimento_item_tipo_catalog_type
    CHECK (catalog_type IN ('PRODUCTS', 'SERVICES'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_item_tipo_tenant_nome
  ON movimento_item_tipo (tenant_id, nome);

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_item_tipo_id_tenant
  ON movimento_item_tipo (id, tenant_id);

CREATE INDEX IF NOT EXISTS idx_movimento_item_tipo_lookup
  ON movimento_item_tipo (tenant_id, catalog_type, ativo, nome);

-- ===== END V19__movimento_item_tipo.sql =====
