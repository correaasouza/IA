-- Codigo sequencial por configuracao para movimento_estoque
-- e codigo sequencial por movimento para movimento_estoque_item.

ALTER TABLE movimento_estoque
  ADD COLUMN IF NOT EXISTS codigo BIGINT;

ALTER TABLE movimento_estoque_item
  ADD COLUMN IF NOT EXISTS codigo BIGINT;

WITH ranked_mov AS (
  SELECT
    me.id,
    row_number() OVER (
      PARTITION BY me.tenant_id, me.movimento_config_id
      ORDER BY me.created_at ASC, me.id ASC
    ) AS next_codigo
  FROM movimento_estoque me
  WHERE me.codigo IS NULL
)
UPDATE movimento_estoque me
SET codigo = ranked_mov.next_codigo
FROM ranked_mov
WHERE me.id = ranked_mov.id;

WITH ranked_item AS (
  SELECT
    mei.id,
    row_number() OVER (
      PARTITION BY mei.tenant_id, mei.movimento_estoque_id
      ORDER BY mei.ordem ASC, mei.id ASC
    ) AS next_codigo
  FROM movimento_estoque_item mei
  WHERE mei.codigo IS NULL
)
UPDATE movimento_estoque_item mei
SET codigo = ranked_item.next_codigo
FROM ranked_item
WHERE mei.id = ranked_item.id;

CREATE TABLE IF NOT EXISTS movimento_estoque_codigo_seq (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  movimento_config_id BIGINT NOT NULL,
  next_value BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by VARCHAR(120),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by VARCHAR(120),
  CONSTRAINT fk_mov_estoque_codigo_seq_config_tenant
    FOREIGN KEY (movimento_config_id, tenant_id)
    REFERENCES movimento_config (id, tenant_id)
    ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_estoque_codigo_seq_scope
  ON movimento_estoque_codigo_seq (tenant_id, movimento_config_id);

INSERT INTO movimento_estoque_codigo_seq (
  tenant_id,
  movimento_config_id,
  next_value,
  created_at,
  created_by,
  updated_at,
  updated_by
)
SELECT
  me.tenant_id,
  me.movimento_config_id,
  COALESCE(MAX(me.codigo), 0) + 1,
  NOW(),
  'flyway',
  NOW(),
  'flyway'
FROM movimento_estoque me
GROUP BY me.tenant_id, me.movimento_config_id
ON CONFLICT (tenant_id, movimento_config_id)
DO UPDATE SET
  next_value = GREATEST(movimento_estoque_codigo_seq.next_value, EXCLUDED.next_value),
  updated_at = NOW(),
  updated_by = 'flyway';

ALTER TABLE movimento_estoque
  ALTER COLUMN codigo SET NOT NULL;

ALTER TABLE movimento_estoque_item
  ALTER COLUMN codigo SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_estoque_codigo_scope
  ON movimento_estoque (tenant_id, movimento_config_id, codigo);

CREATE UNIQUE INDEX IF NOT EXISTS ux_movimento_estoque_item_codigo_scope
  ON movimento_estoque_item (tenant_id, movimento_estoque_id, codigo);
