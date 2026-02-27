INSERT INTO papel (tenant_id, nome, descricao, ativo)
SELECT l.id, 'USER', 'Usuario padrao do locatario', TRUE
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1
  FROM papel p
  WHERE p.tenant_id = l.id
    AND UPPER(p.nome) = 'USER'
);
