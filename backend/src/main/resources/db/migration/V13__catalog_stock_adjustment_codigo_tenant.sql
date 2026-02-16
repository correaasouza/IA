-- ===== BEGIN V13__catalog_stock_adjustment_codigo_tenant.sql =====

DROP INDEX IF EXISTS ux_catalog_stock_adjustment_scope_codigo_active;

CREATE UNIQUE INDEX IF NOT EXISTS ux_catalog_stock_adjustment_tenant_codigo
  ON catalog_stock_adjustment (tenant_id, codigo);

-- ===== END V13__catalog_stock_adjustment_codigo_tenant.sql =====
