DELETE FROM permissao_catalogo
WHERE codigo NOT IN ('MASTER_ADMIN', 'TENANT_ADMIN');
