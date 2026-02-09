INSERT INTO entidade_definicao (tenant_id, codigo, nome, ativo, created_at, updated_at)
VALUES (1, 'CLIENTE', 'Cliente', true, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO entidade_definicao (tenant_id, codigo, nome, ativo, created_at, updated_at)
VALUES (1, 'FUNCIONARIO', 'Funcionário', true, NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO entidade_definicao (tenant_id, codigo, nome, ativo, created_at, updated_at)
VALUES (1, 'FORNECEDOR', 'Fornecedor', true, NOW(), NOW())
ON CONFLICT DO NOTHING;
