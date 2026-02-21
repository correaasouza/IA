-- ===== BEGIN V35__permissao_movimento_estoque_desfazer.sql =====

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo, created_at, updated_at)
VALUES (1, 'MOVIMENTO_ESTOQUE_DESFAZER', 'Desfazer movimentacao de estoque do item', TRUE, NOW(), NOW())
ON CONFLICT (tenant_id, codigo) DO NOTHING;

INSERT INTO papel_permissao (tenant_id, papel_id, permissao_codigo, created_at, updated_at)
SELECT p.tenant_id, p.id, 'MOVIMENTO_ESTOQUE_DESFAZER', NOW(), NOW()
FROM papel p
WHERE p.tenant_id = 1
  AND p.nome IN ('MASTER', 'ADMIN')
  AND NOT EXISTS (
    SELECT 1
    FROM papel_permissao pp
    WHERE pp.tenant_id = p.tenant_id
      AND pp.papel_id = p.id
      AND pp.permissao_codigo = 'MOVIMENTO_ESTOQUE_DESFAZER'
  );

-- ===== END V35__permissao_movimento_estoque_desfazer.sql =====
