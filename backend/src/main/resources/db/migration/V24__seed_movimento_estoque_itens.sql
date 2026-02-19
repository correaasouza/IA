-- ===== BEGIN V24__seed_movimento_estoque_itens.sql =====
-- Seed tecnico: adiciona itens nos movimentos "SEED MOV ESTOQUE ####" (tenant 1).
-- Idempotente: somente movimentos seed sem itens recebem carga.

DO $$
DECLARE
  v_tenant_id BIGINT := 1;
  v_seed_without_items INTEGER := 0;
  v_inserted_items INTEGER := 0;
BEGIN
  SELECT COUNT(*)
    INTO v_seed_without_items
  FROM movimento_estoque me
  WHERE me.tenant_id = v_tenant_id
    AND me.nome LIKE 'SEED MOV ESTOQUE %'
    AND NOT EXISTS (
      SELECT 1
      FROM movimento_estoque_item mei
      WHERE mei.tenant_id = me.tenant_id
        AND mei.movimento_estoque_id = me.id
    );

  IF v_seed_without_items = 0 THEN
    RAISE NOTICE 'V24 seed: nenhum movimento seed sem itens para tenant %.', v_tenant_id;
    RETURN;
  END IF;

  WITH seed_movimentos AS (
    SELECT me.id, me.tenant_id, me.movimento_config_id
    FROM movimento_estoque me
    WHERE me.tenant_id = v_tenant_id
      AND me.nome LIKE 'SEED MOV ESTOQUE %'
      AND NOT EXISTS (
        SELECT 1
        FROM movimento_estoque_item mei
        WHERE mei.tenant_id = me.tenant_id
          AND mei.movimento_estoque_id = me.id
      )
  ),
  tipo_por_movimento AS (
    SELECT
      sm.id AS movimento_estoque_id,
      sm.tenant_id,
      mci.movimento_item_tipo_id,
      mit.catalog_type,
      mci.cobrar
    FROM seed_movimentos sm
    JOIN LATERAL (
      SELECT mci.movimento_item_tipo_id, mci.cobrar
      FROM movimento_config_item_tipo mci
      WHERE mci.tenant_id = sm.tenant_id
        AND mci.movimento_config_id = sm.movimento_config_id
      ORDER BY mci.id
      LIMIT 1
    ) mci ON TRUE
    JOIN movimento_item_tipo mit
      ON mit.id = mci.movimento_item_tipo_id
     AND mit.tenant_id = sm.tenant_id
     AND mit.ativo = TRUE
  ),
  catalogo_por_movimento AS (
    SELECT
      tpm.movimento_estoque_id,
      tpm.tenant_id,
      tpm.movimento_item_tipo_id,
      tpm.catalog_type,
      tpm.cobrar,
      COALESCE(p.catalog_item_id, s.catalog_item_id) AS catalog_item_id,
      COALESCE(p.codigo_snapshot, s.codigo_snapshot) AS codigo_snapshot,
      COALESCE(p.nome_snapshot, s.nome_snapshot) AS nome_snapshot
    FROM tipo_por_movimento tpm
    LEFT JOIN LATERAL (
      SELECT p.id AS catalog_item_id, p.codigo AS codigo_snapshot, p.nome AS nome_snapshot
      FROM catalog_product p
      WHERE tpm.catalog_type = 'PRODUCTS'
        AND p.tenant_id = tpm.tenant_id
        AND p.ativo = TRUE
      ORDER BY p.id
      LIMIT 1
    ) p ON TRUE
    LEFT JOIN LATERAL (
      SELECT s.id AS catalog_item_id, s.codigo AS codigo_snapshot, s.nome AS nome_snapshot
      FROM catalog_service_item s
      WHERE tpm.catalog_type = 'SERVICES'
        AND s.tenant_id = tpm.tenant_id
        AND s.ativo = TRUE
      ORDER BY s.id
      LIMIT 1
    ) s ON TRUE
    WHERE COALESCE(p.catalog_item_id, s.catalog_item_id) IS NOT NULL
  ),
  itens_para_inserir AS (
    SELECT
      cpm.tenant_id,
      cpm.movimento_estoque_id,
      cpm.movimento_item_tipo_id,
      cpm.catalog_type,
      cpm.catalog_item_id,
      cpm.codigo_snapshot,
      cpm.nome_snapshot,
      cpm.cobrar,
      gs.ordem
    FROM catalogo_por_movimento cpm
    CROSS JOIN LATERAL (
      SELECT 0 AS ordem
      UNION ALL
      SELECT 1 AS ordem
    ) gs
  )
  INSERT INTO movimento_estoque_item (
    tenant_id,
    movimento_estoque_id,
    movimento_item_tipo_id,
    catalog_type,
    catalog_item_id,
    catalog_codigo_snapshot,
    catalog_nome_snapshot,
    quantidade,
    valor_unitario,
    valor_total,
    cobrar,
    ordem,
    observacao,
    created_by,
    updated_by
  )
  SELECT
    i.tenant_id,
    i.movimento_estoque_id,
    i.movimento_item_tipo_id,
    i.catalog_type,
    i.catalog_item_id,
    i.codigo_snapshot,
    i.nome_snapshot,
    CASE WHEN i.ordem = 0 THEN 1.000000 ELSE 2.000000 END::numeric(19,6),
    CASE WHEN i.cobrar THEN (CASE WHEN i.ordem = 0 THEN 100.000000 ELSE 150.000000 END)::numeric(19,6) ELSE 0::numeric(19,6) END,
    CASE WHEN i.cobrar THEN (CASE WHEN i.ordem = 0 THEN 100.000000 ELSE 300.000000 END)::numeric(19,6) ELSE 0::numeric(19,6) END,
    i.cobrar,
    i.ordem,
    'Item seed',
    'flyway-seed',
    'flyway-seed'
  FROM itens_para_inserir i;

  GET DIAGNOSTICS v_inserted_items = ROW_COUNT;
  RAISE NOTICE 'V24 seed: inseridos % itens seed para tenant %.', v_inserted_items, v_tenant_id;
END $$;

-- ===== END V24__seed_movimento_estoque_itens.sql =====
