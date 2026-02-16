-- ===== BEGIN V6__catalog_items.sql =====

CREATE TABLE IF NOT EXISTS catalog_product (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  agrupador_empresa_id BIGINT NOT NULL,
  catalog_group_id BIGINT,
  codigo BIGINT NOT NULL,
  nome VARCHAR(200) NOT NULL,
  descricao VARCHAR(255),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_catalog_product_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_catalog_product_agrupador_tenant
    FOREIGN KEY (agrupador_empresa_id, tenant_id)
    REFERENCES agrupador_empresa (id, tenant_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_catalog_product_group_scope
    FOREIGN KEY (catalog_group_id, tenant_id, catalog_configuration_id)
    REFERENCES catalog_group (id, tenant_id, catalog_configuration_id)
    ON DELETE SET NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_product_codigo_scope
  ON catalog_product (tenant_id, catalog_configuration_id, agrupador_empresa_id, codigo);

CREATE INDEX IF NOT EXISTS idx_catalog_product_scope_ativo_codigo
  ON catalog_product (tenant_id, catalog_configuration_id, agrupador_empresa_id, ativo, codigo);

CREATE INDEX IF NOT EXISTS idx_catalog_product_scope_group
  ON catalog_product (tenant_id, catalog_configuration_id, agrupador_empresa_id, catalog_group_id);

CREATE TABLE IF NOT EXISTS catalog_service_item (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_configuration_id BIGINT NOT NULL,
  agrupador_empresa_id BIGINT NOT NULL,
  catalog_group_id BIGINT,
  codigo BIGINT NOT NULL,
  nome VARCHAR(200) NOT NULL,
  descricao VARCHAR(255),
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_catalog_service_catalog_scope
    FOREIGN KEY (catalog_configuration_id, tenant_id)
    REFERENCES catalog_configuration (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_catalog_service_agrupador_tenant
    FOREIGN KEY (agrupador_empresa_id, tenant_id)
    REFERENCES agrupador_empresa (id, tenant_id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_catalog_service_group_scope
    FOREIGN KEY (catalog_group_id, tenant_id, catalog_configuration_id)
    REFERENCES catalog_group (id, tenant_id, catalog_configuration_id)
    ON DELETE SET NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_service_item_codigo_scope
  ON catalog_service_item (tenant_id, catalog_configuration_id, agrupador_empresa_id, codigo);

CREATE INDEX IF NOT EXISTS idx_catalog_service_scope_ativo_codigo
  ON catalog_service_item (tenant_id, catalog_configuration_id, agrupador_empresa_id, ativo, codigo);

CREATE INDEX IF NOT EXISTS idx_catalog_service_scope_group
  ON catalog_service_item (tenant_id, catalog_configuration_id, agrupador_empresa_id, catalog_group_id);

-- ===== END V6__catalog_items.sql =====
