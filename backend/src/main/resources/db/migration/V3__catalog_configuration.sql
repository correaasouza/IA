-- ===== BEGIN V3__catalog_configuration.sql =====

CREATE TABLE IF NOT EXISTS catalog_configuration (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  type VARCHAR(20) NOT NULL,
  numbering_mode VARCHAR(20) NOT NULL DEFAULT 'AUTOMATICA',
  active BOOLEAN NOT NULL DEFAULT TRUE,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_catalog_configuration_type
    CHECK (type IN ('PRODUCTS', 'SERVICES')),
  CONSTRAINT ck_catalog_configuration_numbering_mode
    CHECK (numbering_mode IN ('AUTOMATICA', 'MANUAL'))
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_configuration_tenant_type
  ON catalog_configuration (tenant_id, type);

-- ===== END V3__catalog_configuration.sql =====
