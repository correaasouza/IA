-- ===== BEGIN V23__seed_movimento_estoque_200.sql =====
-- Seed tecnico para ambiente local: garante 200 movimentos de estoque no tenant 1.
-- Idempotente por prefixo de nome, evitando duplicacao em reexecucoes manuais.

DO $$
DECLARE
  v_tenant_id BIGINT := 1;
  v_empresa_id BIGINT;
  v_config_id BIGINT;
  v_tipo_entidade_padrao_id BIGINT;
  v_existing_count INTEGER := 0;
  v_to_insert INTEGER := 0;
BEGIN
  SELECT mce.empresa_id, mc.id, mc.tipo_entidade_padrao_id
    INTO v_empresa_id, v_config_id, v_tipo_entidade_padrao_id
  FROM movimento_config mc
  JOIN movimento_config_empresa mce
    ON mce.movimento_config_id = mc.id
   AND mce.tenant_id = mc.tenant_id
  WHERE mc.tenant_id = v_tenant_id
    AND mc.ativo = TRUE
    AND mc.movimento_tipo = 'MOVIMENTO_ESTOQUE'
  ORDER BY mc.prioridade DESC, mc.updated_at DESC, mc.id DESC
  LIMIT 1;

  IF v_empresa_id IS NULL OR v_config_id IS NULL THEN
    RAISE NOTICE 'V23 seed ignorado: sem configuracao MOVIMENTO_ESTOQUE ativa para tenant %.', v_tenant_id;
    RETURN;
  END IF;

  SELECT COUNT(*)
    INTO v_existing_count
  FROM movimento_estoque me
  WHERE me.tenant_id = v_tenant_id
    AND me.empresa_id = v_empresa_id
    AND me.nome LIKE 'SEED MOV ESTOQUE %';

  v_to_insert := GREATEST(0, 200 - v_existing_count);

  IF v_to_insert = 0 THEN
    RAISE NOTICE 'V23 seed: ja existem 200 movimentos SEED para tenant %, empresa %.', v_tenant_id, v_empresa_id;
    RETURN;
  END IF;

  INSERT INTO movimento_estoque (
    tenant_id,
    empresa_id,
    tipo_movimento,
    status,
    nome,
    movimento_config_id,
    tipo_entidade_padrao_id,
    version,
    created_by,
    updated_by
  )
  SELECT
    v_tenant_id,
    v_empresa_id,
    'MOVIMENTO_ESTOQUE',
    'ABERTO',
    'SEED MOV ESTOQUE ' || LPAD((v_existing_count + gs)::TEXT, 4, '0'),
    v_config_id,
    v_tipo_entidade_padrao_id,
    0,
    'flyway-seed',
    'flyway-seed'
  FROM generate_series(1, v_to_insert) gs;

  RAISE NOTICE 'V23 seed: inseridos % movimentos para tenant %, empresa %.', v_to_insert, v_tenant_id, v_empresa_id;
END $$;

-- ===== END V23__seed_movimento_estoque_200.sql =====
