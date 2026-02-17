-- ===== BEGIN V15__movimento_config_tipo_entidade_padrao_nullable.sql =====

ALTER TABLE movimento_config
  ALTER COLUMN tipo_entidade_padrao_id DROP NOT NULL;

-- ===== END V15__movimento_config_tipo_entidade_padrao_nullable.sql =====
