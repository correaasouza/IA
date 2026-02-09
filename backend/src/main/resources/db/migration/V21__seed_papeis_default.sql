INSERT INTO papel (tenant_id, nome, descricao, ativo)
SELECT l.id, 'ADMIN', 'Administrador do locatário', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM papel p WHERE p.tenant_id = l.id AND p.nome = 'ADMIN'
);

INSERT INTO papel (tenant_id, nome, descricao, ativo)
SELECT l.id, 'USUARIO', 'Usuário padrão', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM papel p WHERE p.tenant_id = l.id AND p.nome = 'USUARIO'
);

INSERT INTO papel_permissao (tenant_id, papel_id, permissao_codigo)
SELECT p.tenant_id, p.id, pc.codigo
FROM papel p
JOIN permissao_catalogo pc ON pc.tenant_id = p.tenant_id
WHERE p.nome = 'ADMIN'
  AND pc.codigo IN ('CONFIG_EDITOR','USUARIO_MANAGE','PAPEL_MANAGE','RELATORIO_VIEW','ENTIDADE_EDIT')
  AND NOT EXISTS (
    SELECT 1 FROM papel_permissao pp
    WHERE pp.papel_id = p.id AND pp.permissao_codigo = pc.codigo
  );
