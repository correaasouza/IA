INSERT INTO tipo_entidade (tenant_id, codigo, nome, ativo, versao, created_at, updated_at)
SELECT l.id, 'CLIENTE', 'Cliente', TRUE, 1, NOW(), NOW()
FROM locatario l
ON CONFLICT (tenant_id, codigo) DO NOTHING;

INSERT INTO tipo_entidade (tenant_id, codigo, nome, ativo, versao, created_at, updated_at)
SELECT l.id, 'FORNECEDOR', 'Fornecedor', TRUE, 1, NOW(), NOW()
FROM locatario l
ON CONFLICT (tenant_id, codigo) DO NOTHING;

INSERT INTO tipo_entidade (tenant_id, codigo, nome, ativo, versao, created_at, updated_at)
SELECT l.id, 'FUNCIONARIO', 'Funcionário', TRUE, 1, NOW(), NOW()
FROM locatario l
ON CONFLICT (tenant_id, codigo) DO NOTHING;
