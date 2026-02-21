CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_catalog_product_search_text_trgm
  ON catalog_product
  USING gin (lower(coalesce(nome, '') || ' ' || coalesce(descricao, '')) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_catalog_service_item_search_text_trgm
  ON catalog_service_item
  USING gin (lower(coalesce(nome, '') || ' ' || coalesce(descricao, '')) gin_trgm_ops);
