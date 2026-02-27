ALTER TABLE tenant_unit
  ADD COLUMN padrao boolean NOT NULL DEFAULT false;

WITH ranked AS (
  SELECT
    id,
    tenant_id,
    row_number() OVER (
      PARTITION BY tenant_id
      ORDER BY
        CASE WHEN upper(coalesce(sigla, '')) = 'UN' THEN 0 ELSE 1 END,
        upper(coalesce(sigla, '')),
        id
    ) AS rn
  FROM tenant_unit
)
UPDATE tenant_unit tu
SET padrao = true
FROM ranked r
WHERE tu.id = r.id
  AND r.rn = 1;

CREATE UNIQUE INDEX uk_tenant_unit_default_per_tenant
  ON tenant_unit (tenant_id)
  WHERE padrao;
