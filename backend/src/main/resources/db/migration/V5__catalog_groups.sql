-- ===== BEGIN V5__catalog_groups.sql =====

CREATE TABLE IF NOT EXISTS catalog_group (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  parent_id BIGINT,
  nome VARCHAR(120) NOT NULL,
  nome_normalizado VARCHAR(120) NOT NULL,
  nivel INTEGER NOT NULL DEFAULT 0,
  path VARCHAR(900) NOT NULL,
  ordem INTEGER NOT NULL DEFAULT 0,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_catalog_group_nivel CHECK (nivel >= 0),
  CONSTRAINT ck_catalog_group_ordem CHECK (ordem >= 0),
  CONSTRAINT fk_catalog_group_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_group_id_scope
  ON catalog_group (id, tenant_id, catalog_configuration_id);

ALTER TABLE catalog_group
  ADD CONSTRAINT fk_catalog_group_parent_scope
    FOREIGN KEY (parent_id, tenant_id, catalog_configuration_id)
    REFERENCES catalog_group (id, tenant_id, catalog_configuration_id)
    ON DELETE RESTRICT;

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_group_nome_parent_ativo
  ON catalog_group (
    tenant_id,
    catalog_configuration_id,
    COALESCE(parent_id, 0),
    nome_normalizado,
    ativo
  );

CREATE INDEX IF NOT EXISTS idx_catalog_group_scope_parent_ordem
  ON catalog_group (tenant_id, catalog_configuration_id, parent_id, ordem);

CREATE INDEX IF NOT EXISTS idx_catalog_group_scope_path
  ON catalog_group (tenant_id, catalog_configuration_id, path);

-- ===== END V5__catalog_groups.sql =====
