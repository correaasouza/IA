-- ===== BEGIN V4__catalog_configuration_group.sql =====

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_configuration_id_tenant
  ON catalog_configuration (id, tenant_id);

CREATE TABLE IF NOT EXISTS catalog_configuration_by_group (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  agrupador_id BIGINT NOT NULL,
  numbering_mode VARCHAR(20) NOT NULL DEFAULT 'AUTOMATICA',
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_catalog_cfg_group_numbering_mode
    CHECK (numbering_mode IN ('AUTOMATICA', 'MANUAL')),
  CONSTRAINT fk_catalog_cfg_group_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_catalog_cfg_group_agrupador_scope
    FOREIGN KEY (agrupador_id, tenant_id)
    REFERENCES agrupador_empresa (id, tenant_id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_cfg_group_active
  ON catalog_configuration_by_group (tenant_id, catalog_configuration_id, agrupador_id, active);

CREATE INDEX IF NOT EXISTS idx_catalog_cfg_group_scope
  ON catalog_configuration_by_group (tenant_id, catalog_configuration_id, active);

-- ===== END V4__catalog_configuration_group.sql =====
