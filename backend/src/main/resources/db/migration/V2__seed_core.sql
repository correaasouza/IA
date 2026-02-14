INSERT INTO locatario (id, nome, data_limite_acesso, ativo, created_at, updated_at)
VALUES (1, 'Master', '2099-12-31', TRUE, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO permissao_catalogo (tenant_id, codigo, label, ativo)
VALUES
  (1, 'MASTER_ADMIN', 'Master Admin', TRUE),
  (1, 'TENANT_ADMIN', 'Admin do Locatario', TRUE),
  (1, 'CONFIG_EDITOR', 'Configurar colunas e formularios', TRUE),
  (1, 'USUARIO_MANAGE', 'Gerenciar usuarios', TRUE),
  (1, 'PAPEL_MANAGE', 'Gerenciar papeis', TRUE),
  (1, 'RELATORIO_VIEW', 'Visualizar relatorios', TRUE),
  (1, 'ENTIDADE_EDIT', 'Editar entidades', TRUE)
ON CONFLICT (tenant_id, codigo) DO NOTHING;

INSERT INTO papel (tenant_id, nome, descricao, ativo)
VALUES
  (1, 'ADMIN', 'Administrador do locatario', TRUE),
  (1, 'USUARIO', 'Usuario padrao', TRUE)
ON CONFLICT (tenant_id, nome) DO NOTHING;

INSERT INTO papel_permissao (tenant_id, papel_id, permissao_codigo)
SELECT p.tenant_id, p.id, pc.codigo
FROM papel p
JOIN permissao_catalogo pc ON pc.tenant_id = p.tenant_id
WHERE p.tenant_id = 1
  AND p.nome = 'ADMIN'
  AND pc.codigo IN ('CONFIG_EDITOR', 'USUARIO_MANAGE', 'PAPEL_MANAGE', 'RELATORIO_VIEW')
  AND NOT EXISTS (
    SELECT 1
    FROM papel_permissao pp
    WHERE pp.papel_id = p.id
      AND pp.permissao_codigo = pc.codigo
  );
