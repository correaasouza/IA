-- ===== BEGIN V36__catalog_prices.sql =====

CREATE TABLE IF NOT EXISTS catalog_price_rule_by_group (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_configuration_by_group_id BIGINT NOT NULL,
  price_type VARCHAR(20) NOT NULL,
  custom_name VARCHAR(80),
  base_mode VARCHAR(20) NOT NULL,
  base_price_type VARCHAR(20),
  adjustment_kind_default VARCHAR(20) NOT NULL,
  adjustment_default NUMERIC(19,6) NOT NULL DEFAULT 0,
  ui_lock_mode VARCHAR(10) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_catalog_price_rule_type CHECK (price_type IN ('PURCHASE', 'COST', 'AVERAGE_COST', 'SALE_BASE')),
  CONSTRAINT ck_catalog_price_rule_base_mode CHECK (base_mode IN ('NONE', 'BASE_PRICE')),
  CONSTRAINT ck_catalog_price_rule_base_type CHECK (
    (base_mode = 'NONE' AND base_price_type IS NULL)
    OR
    (base_mode = 'BASE_PRICE' AND base_price_type IN ('PURCHASE', 'COST', 'AVERAGE_COST', 'SALE_BASE'))
  ),
  CONSTRAINT ck_catalog_price_rule_adjust_kind CHECK (adjustment_kind_default IN ('FIXED', 'PERCENT')),
  CONSTRAINT ck_catalog_price_rule_lock_mode CHECK (ui_lock_mode IN ('I', 'II', 'III', 'IV')),
  CONSTRAINT fk_catalog_price_rule_group_cfg
    FOREIGN KEY (catalog_configuration_by_group_id)
    REFERENCES catalog_configuration_by_group (id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_price_rule_group_type
  ON catalog_price_rule_by_group (tenant_id, catalog_configuration_by_group_id, price_type);

CREATE INDEX IF NOT EXISTS idx_catalog_price_rule_group
  ON catalog_price_rule_by_group (tenant_id, catalog_configuration_by_group_id, active);

CREATE TABLE IF NOT EXISTS catalog_item_price (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  catalog_type VARCHAR(20) NOT NULL,
  catalog_item_id BIGINT NOT NULL,
  price_type VARCHAR(20) NOT NULL,
  price_final NUMERIC(19,6) NOT NULL DEFAULT 0,
  adjustment_kind VARCHAR(20) NOT NULL,
  adjustment_value NUMERIC(19,6) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_catalog_item_price_catalog_type CHECK (catalog_type IN ('PRODUCTS', 'SERVICES')),
  CONSTRAINT ck_catalog_item_price_type CHECK (price_type IN ('PURCHASE', 'COST', 'AVERAGE_COST', 'SALE_BASE')),
  CONSTRAINT ck_catalog_item_price_adjust_kind CHECK (adjustment_kind IN ('FIXED', 'PERCENT')),
  CONSTRAINT ck_catalog_item_price_non_negative CHECK (price_final >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_item_price_scope
  ON catalog_item_price (tenant_id, catalog_type, catalog_item_id, price_type);

CREATE INDEX IF NOT EXISTS idx_catalog_item_price_item
  ON catalog_item_price (tenant_id, catalog_type, catalog_item_id);

CREATE TABLE IF NOT EXISTS price_book (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  is_default BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_price_book_tenant_name
  ON price_book (tenant_id, lower(name));

CREATE UNIQUE INDEX IF NOT EXISTS ux_price_book_tenant_default
  ON price_book (tenant_id)
  WHERE is_default = TRUE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_price_book_id_tenant
  ON price_book (id, tenant_id);

CREATE TABLE IF NOT EXISTS price_variant (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  name VARCHAR(120) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_price_variant_tenant_name
  ON price_variant (tenant_id, lower(name));

CREATE UNIQUE INDEX IF NOT EXISTS ux_price_variant_id_tenant
  ON price_variant (id, tenant_id);

CREATE TABLE IF NOT EXISTS sale_price (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  price_book_id BIGINT NOT NULL,
  variant_id BIGINT,
  catalog_type VARCHAR(20) NOT NULL,
  catalog_item_id BIGINT NOT NULL,
  tenant_unit_id UUID,
  price_final NUMERIC(19,6) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_sale_price_catalog_type CHECK (catalog_type IN ('PRODUCTS', 'SERVICES')),
  CONSTRAINT ck_sale_price_non_negative CHECK (price_final >= 0),
  CONSTRAINT fk_sale_price_book_tenant
    FOREIGN KEY (price_book_id, tenant_id)
    REFERENCES price_book (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_sale_price_variant_tenant
    FOREIGN KEY (variant_id, tenant_id)
    REFERENCES price_variant (id, tenant_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_sale_price_tenant_unit
    FOREIGN KEY (tenant_unit_id, tenant_id)
    REFERENCES tenant_unit (id, tenant_id)
    ON DELETE SET NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_sale_price_scope
  ON sale_price (
    tenant_id,
    price_book_id,
    COALESCE(variant_id, 0),
    catalog_type,
    catalog_item_id,
    COALESCE(tenant_unit_id, '00000000-0000-0000-0000-000000000000'::UUID)
  );

CREATE INDEX IF NOT EXISTS idx_sale_price_lookup
  ON sale_price (tenant_id, price_book_id, variant_id, catalog_type, catalog_item_id, tenant_unit_id);

CREATE TABLE IF NOT EXISTS price_change_log (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  sale_price_id BIGINT,
  action VARCHAR(20) NOT NULL,
  old_price_final NUMERIC(19,6),
  new_price_final NUMERIC(19,6),
  price_book_id BIGINT,
  variant_id BIGINT,
  catalog_type VARCHAR(20) NOT NULL,
  catalog_item_id BIGINT NOT NULL,
  tenant_unit_id UUID,
  changed_by VARCHAR(120),
  changed_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT ck_price_change_log_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE')),
  CONSTRAINT ck_price_change_log_catalog_type CHECK (catalog_type IN ('PRODUCTS', 'SERVICES'))
);

CREATE INDEX IF NOT EXISTS idx_price_change_log_scope
  ON price_change_log (tenant_id, price_book_id, variant_id, catalog_type, catalog_item_id, changed_at DESC);

ALTER TABLE movimento_estoque_item
  ADD COLUMN IF NOT EXISTS unit_price_applied NUMERIC(19,6),
  ADD COLUMN IF NOT EXISTS price_book_id_snapshot BIGINT,
  ADD COLUMN IF NOT EXISTS variant_id_snapshot BIGINT,
  ADD COLUMN IF NOT EXISTS sale_price_source_snapshot VARCHAR(40),
  ADD COLUMN IF NOT EXISTS sale_price_id_snapshot BIGINT;

UPDATE movimento_estoque_item
SET unit_price_applied = COALESCE(valor_unitario, 0)
WHERE unit_price_applied IS NULL;

ALTER TABLE movimento_estoque_item
  ALTER COLUMN unit_price_applied SET NOT NULL;

ALTER TABLE movimento_estoque_item
  ADD CONSTRAINT ck_mov_estoque_item_unit_price_applied_non_negative
    CHECK (unit_price_applied >= 0);

ALTER TABLE movimento_estoque_item
  DROP CONSTRAINT IF EXISTS fk_mov_estoque_item_price_book_snapshot;

ALTER TABLE movimento_estoque_item
  ADD CONSTRAINT fk_mov_estoque_item_price_book_snapshot
  FOREIGN KEY (price_book_id_snapshot, tenant_id)
  REFERENCES price_book (id, tenant_id)
  ON DELETE SET NULL;

ALTER TABLE movimento_estoque_item
  DROP CONSTRAINT IF EXISTS fk_mov_estoque_item_variant_snapshot;

ALTER TABLE movimento_estoque_item
  ADD CONSTRAINT fk_mov_estoque_item_variant_snapshot
  FOREIGN KEY (variant_id_snapshot, tenant_id)
  REFERENCES price_variant (id, tenant_id)
  ON DELETE SET NULL;

ALTER TABLE movimento_estoque_item
  DROP CONSTRAINT IF EXISTS fk_mov_estoque_item_sale_price_snapshot;

ALTER TABLE movimento_estoque_item
  ADD CONSTRAINT fk_mov_estoque_item_sale_price_snapshot
  FOREIGN KEY (sale_price_id_snapshot)
  REFERENCES sale_price (id)
  ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_mov_estoque_item_price_snapshot
  ON movimento_estoque_item (tenant_id, price_book_id_snapshot, variant_id_snapshot, catalog_type, catalog_item_id);

ALTER TABLE registro_entidade
  ADD COLUMN IF NOT EXISTS price_book_id BIGINT;

ALTER TABLE registro_entidade
  DROP CONSTRAINT IF EXISTS fk_registro_entidade_price_book;

ALTER TABLE registro_entidade
  ADD CONSTRAINT fk_registro_entidade_price_book
  FOREIGN KEY (price_book_id, tenant_id)
  REFERENCES price_book (id, tenant_id)
  ON DELETE SET NULL;

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo, created_at, updated_at)
VALUES
  (1, 'CATALOG_PRICES_VIEW', 'Visualizar precos de catalogo', TRUE, NOW(), NOW()),
  (1, 'CATALOG_PRICES_MANAGE', 'Gerenciar precos de catalogo', TRUE, NOW(), NOW())
ON CONFLICT (tenant_id, codigo) DO NOTHING;

INSERT INTO papel_permissao (tenant_id, papel_id, permissao_codigo, created_at, updated_at)
SELECT p.tenant_id, p.id, pc.codigo, NOW(), NOW()
FROM papel p
JOIN permissao_catalogo pc ON pc.tenant_id = p.tenant_id
WHERE p.tenant_id = 1
  AND p.nome IN ('MASTER', 'ADMIN')
  AND pc.codigo IN ('CATALOG_PRICES_VIEW', 'CATALOG_PRICES_MANAGE')
  AND NOT EXISTS (
    SELECT 1
    FROM papel_permissao pp
    WHERE pp.tenant_id = p.tenant_id
      AND pp.papel_id = p.id
      AND pp.permissao_codigo = pc.codigo
  );

-- ===== END V36__catalog_prices.sql =====
