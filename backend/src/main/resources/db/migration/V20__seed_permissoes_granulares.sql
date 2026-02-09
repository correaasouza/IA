INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
SELECT l.id, 'CONFIG_EDITOR', 'Configurar colunas e formulários', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM permissao_catalogo p WHERE p.tenant_id = l.id AND p.codigo = 'CONFIG_EDITOR'
);

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
SELECT l.id, 'USUARIO_MANAGE', 'Gerenciar usuários', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM permissao_catalogo p WHERE p.tenant_id = l.id AND p.codigo = 'USUARIO_MANAGE'
);

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
SELECT l.id, 'PAPEL_MANAGE', 'Gerenciar papéis', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM permissao_catalogo p WHERE p.tenant_id = l.id AND p.codigo = 'PAPEL_MANAGE'
);

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
SELECT l.id, 'RELATORIO_VIEW', 'Visualizar relatórios', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM permissao_catalogo p WHERE p.tenant_id = l.id AND p.codigo = 'RELATORIO_VIEW'
);

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
SELECT l.id, 'ENTIDADE_EDIT', 'Editar entidades', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM permissao_catalogo p WHERE p.tenant_id = l.id AND p.codigo = 'ENTIDADE_EDIT'
);
