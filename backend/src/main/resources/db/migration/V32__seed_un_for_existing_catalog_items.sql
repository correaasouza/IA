-- Backfill de unidade base "UN" para itens de catalogo legados (produtos e servicos).
-- Regras:
-- 1) Garante a unidade oficial "UN".
-- 2) Para tenants com itens sem unidade base, garante uma tenant_unit vinculada a "UN".
-- 3) Preenche tenant_unit_id nulo em catalog_product e catalog_service_item.

WITH ensure_official_un AS (
  INSERT INTO official_unit (
    id,
    codigo_oficial,
    descricao,
    ativo,
    origem,
    created_at,
    created_by,
    updated_at,
    updated_by
  )
  SELECT
    (
      substr(md5('official_unit:UN'), 1, 8) || '-' ||
      substr(md5('official_unit:UN'), 9, 4) || '-' ||
      substr(md5('official_unit:UN'), 13, 4) || '-' ||
      substr(md5('official_unit:UN'), 17, 4) || '-' ||
      substr(md5('official_unit:UN'), 21, 12)
    )::uuid,
    'UN',
    'UNIDADE',
    TRUE,
    'NFE_TABELA_UNIDADE_COMERCIAL',
    NOW(),
    'flyway',
    NOW(),
    'flyway'
  WHERE NOT EXISTS (
    SELECT 1
    FROM official_unit
    WHERE upper(codigo_oficial) = 'UN'
  )
),
official_un AS (
  SELECT id, descricao
  FROM official_unit
  WHERE upper(codigo_oficial) = 'UN'
  ORDER BY created_at ASC, id ASC
  LIMIT 1
),
target_tenants AS (
  SELECT DISTINCT tenant_id
  FROM catalog_product
  WHERE tenant_unit_id IS NULL
  UNION
  SELECT DISTINCT tenant_id
  FROM catalog_service_item
  WHERE tenant_unit_id IS NULL
),
missing_tenant_un AS (
  SELECT t.tenant_id
  FROM target_tenants t
  CROSS JOIN official_un ou
  WHERE NOT EXISTS (
    SELECT 1
    FROM tenant_unit tu
    WHERE tu.tenant_id = t.tenant_id
      AND tu.unidade_oficial_id = ou.id
  )
)
INSERT INTO tenant_unit (
  id,
  tenant_id,
  unidade_oficial_id,
  sigla,
  nome,
  fator_para_oficial,
  system_mirror,
  created_at,
  created_by,
  updated_at,
  updated_by
)
SELECT
  (
    substr(md5('tenant_unit:' || m.tenant_id::text || ':UN'), 1, 8) || '-' ||
    substr(md5('tenant_unit:' || m.tenant_id::text || ':UN'), 9, 4) || '-' ||
    substr(md5('tenant_unit:' || m.tenant_id::text || ':UN'), 13, 4) || '-' ||
    substr(md5('tenant_unit:' || m.tenant_id::text || ':UN'), 17, 4) || '-' ||
    substr(md5('tenant_unit:' || m.tenant_id::text || ':UN'), 21, 12)
  )::uuid,
  m.tenant_id,
  ou.id,
  CASE
    WHEN NOT EXISTS (
      SELECT 1
      FROM tenant_unit tu
      WHERE tu.tenant_id = m.tenant_id
        AND lower(tu.sigla) = 'un'
    ) THEN 'UN'
    WHEN NOT EXISTS (
      SELECT 1
      FROM tenant_unit tu
      WHERE tu.tenant_id = m.tenant_id
        AND lower(tu.sigla) = 'unof'
    ) THEN 'UNOF'
    WHEN NOT EXISTS (
      SELECT 1
      FROM tenant_unit tu
      WHERE tu.tenant_id = m.tenant_id
        AND lower(tu.sigla) = lower(left('UNOF' || m.tenant_id::text, 20))
    ) THEN left('UNOF' || m.tenant_id::text, 20)
    ELSE left('UN' || substr(md5('sigla:tenant:' || m.tenant_id::text || ':official:UN'), 1, 18), 20)
  END,
  COALESCE(NULLIF(trim(ou.descricao), ''), 'UNIDADE'),
  1.000000000000,
  TRUE,
  NOW(),
  'flyway',
  NOW(),
  'flyway'
FROM missing_tenant_un m
CROSS JOIN official_un ou;

UPDATE catalog_product cp
SET tenant_unit_id = tu.id
FROM (
  SELECT
    tu.tenant_id,
    tu.id,
    row_number() OVER (
      PARTITION BY tu.tenant_id
      ORDER BY tu.system_mirror DESC, tu.updated_at DESC, tu.created_at DESC, tu.id ASC
    ) AS rn
  FROM tenant_unit tu
  JOIN official_unit ou
    ON ou.id = tu.unidade_oficial_id
   AND upper(ou.codigo_oficial) = 'UN'
) tu
WHERE cp.tenant_unit_id IS NULL
  AND cp.tenant_id = tu.tenant_id
  AND tu.rn = 1;

UPDATE catalog_service_item cs
SET tenant_unit_id = tu.id
FROM (
  SELECT
    tu.tenant_id,
    tu.id,
    row_number() OVER (
      PARTITION BY tu.tenant_id
      ORDER BY tu.system_mirror DESC, tu.updated_at DESC, tu.created_at DESC, tu.id ASC
    ) AS rn
  FROM tenant_unit tu
  JOIN official_unit ou
    ON ou.id = tu.unidade_oficial_id
   AND upper(ou.codigo_oficial) = 'UN'
) tu
WHERE cs.tenant_unit_id IS NULL
  AND cs.tenant_id = tu.tenant_id
  AND tu.rn = 1;
