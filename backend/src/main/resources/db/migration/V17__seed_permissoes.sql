INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
SELECT l.id, 'MASTER_ADMIN', 'Master Admin', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM permissao_catalogo p WHERE p.tenant_id = l.id AND p.codigo = 'MASTER_ADMIN'
);

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
SELECT l.id, 'TENANT_ADMIN', 'Admin do Locatário', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM permissao_catalogo p WHERE p.tenant_id = l.id AND p.codigo = 'TENANT_ADMIN'
);
