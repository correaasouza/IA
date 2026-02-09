INSERT INTO contato_tipo (tenant_id, codigo, nome, ativo, obrigatorio, principal_unico, created_at, updated_at)
VALUES (1, 'TELEFONE', 'Telefone', true, false, true, NOW(), NOW())
ON CONFLICT DO NOTHING;
INSERT INTO contato_tipo (tenant_id, codigo, nome, ativo, obrigatorio, principal_unico, created_at, updated_at)
VALUES (1, 'WHATSAPP', 'WhatsApp', true, false, true, NOW(), NOW())
ON CONFLICT DO NOTHING;
INSERT INTO contato_tipo (tenant_id, codigo, nome, ativo, obrigatorio, principal_unico, created_at, updated_at)
VALUES (1, 'EMAIL', 'E-mail', true, false, true, NOW(), NOW())
ON CONFLICT DO NOTHING;
