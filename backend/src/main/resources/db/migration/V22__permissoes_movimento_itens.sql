-- ===== BEGIN V22__permissoes_movimento_itens.sql =====

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo, created_at, updated_at)
VALUES
  (1, 'MOVIMENTO_ITEM_CONFIGURAR', 'Configurar tipos de itens de movimento', TRUE, NOW(), NOW()),
  (1, 'MOVIMENTO_ESTOQUE_ITEM_OPERAR', 'Operar itens no movimento de estoque', TRUE, NOW(), NOW())
ON CONFLICT (tenant_id, codigo) DO NOTHING;

INSERT INTO papel_permissao (tenant_id, papel_id, permissao_codigo, created_at, updated_at)
SELECT p.tenant_id, p.id, x.codigo, NOW(), NOW()
FROM papel p
JOIN (
  SELECT 'MOVIMENTO_ITEM_CONFIGURAR' AS codigo
  UNION ALL
  SELECT 'MOVIMENTO_ESTOQUE_ITEM_OPERAR' AS codigo
) x ON TRUE
WHERE p.tenant_id = 1
  AND p.nome IN ('MASTER', 'ADMIN')
  AND NOT EXISTS (
    SELECT 1
    FROM papel_permissao pp
    WHERE pp.tenant_id = p.tenant_id
      AND pp.papel_id = p.id
      AND pp.permissao_codigo = x.codigo
  );

-- ===== END V22__permissoes_movimento_itens.sql =====
