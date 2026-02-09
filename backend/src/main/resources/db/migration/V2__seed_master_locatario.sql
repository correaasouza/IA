INSERT INTO locatario (id, nome, data_limite_acesso, ativo, created_at, updated_at)
VALUES (1, 'Master', '2099-12-31', true, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;
