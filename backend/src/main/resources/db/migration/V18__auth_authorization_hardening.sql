-- Base roles per tenant (MASTER, ADMIN, USER)
INSERT INTO papel (tenant_id, nome, descricao, ativo, created_at, updated_at)
SELECT l.id, 'MASTER', 'Master do locatario', TRUE, now(), now()
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM papel p WHERE p.tenant_id = l.id AND upper(p.nome) = 'MASTER'
);

INSERT INTO papel (tenant_id, nome, descricao, ativo, created_at, updated_at)
SELECT l.id, 'ADMIN', 'Administrador do locatario', TRUE, now(), now()
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM papel p WHERE p.tenant_id = l.id AND upper(p.nome) = 'ADMIN'
);

INSERT INTO papel (tenant_id, nome, descricao, ativo, created_at, updated_at)
SELECT l.id, 'USER', 'Usuario padrao do locatario', TRUE, now(), now()
FROM locatario l
WHERE NOT EXISTS (
  SELECT 1 FROM papel p WHERE p.tenant_id = l.id AND upper(p.nome) = 'USER'
);

-- Global master invariant: tenant 1 + username master must always keep MASTER role
INSERT INTO usuario_papel (tenant_id, usuario_id, papel_id, created_at, updated_at)
SELECT 1, u.keycloak_id, p.id, now(), now()
FROM usuario u
JOIN papel p ON p.tenant_id = 1 AND upper(p.nome) = 'MASTER'
WHERE u.tenant_id = 1
  AND lower(u.username) = 'master'
  AND NOT EXISTS (
    SELECT 1
    FROM usuario_papel up
    WHERE up.tenant_id = 1
      AND up.usuario_id = u.keycloak_id
      AND up.papel_id = p.id
  );

-- Remove invalid explicit company links (cross-tenant)
DELETE FROM usuario_empresa_acesso uea
WHERE NOT EXISTS (
  SELECT 1
  FROM empresa e
  WHERE e.id = uea.empresa_id
    AND e.tenant_id = uea.tenant_id
);

-- Remove invalid default company references (company missing or cross-tenant)
DELETE FROM usuario_empresa_preferencia uep
WHERE NOT EXISTS (
  SELECT 1
  FROM empresa e
  WHERE e.id = uep.empresa_padrao_id
    AND e.tenant_id = uep.tenant_id
);

-- For users without valid default company, set first accessible company.
-- ADMIN/MASTER derive access from all tenant companies.
WITH role_admin_master AS (
  SELECT DISTINCT up.tenant_id, up.usuario_id
  FROM usuario_papel up
  JOIN papel p ON p.id = up.papel_id
  WHERE upper(p.nome) IN ('ADMIN', 'MASTER')
),
explicit_companies AS (
  SELECT uea.tenant_id, uea.usuario_id, min(uea.empresa_id) AS empresa_id
  FROM usuario_empresa_acesso uea
  GROUP BY uea.tenant_id, uea.usuario_id
),
derived_companies AS (
  SELECT ram.tenant_id, ram.usuario_id, min(e.id) AS empresa_id
  FROM role_admin_master ram
  JOIN empresa e ON e.tenant_id = ram.tenant_id
  GROUP BY ram.tenant_id, ram.usuario_id
),
candidate_default AS (
  SELECT tenant_id, usuario_id, empresa_id FROM explicit_companies
  UNION
  SELECT tenant_id, usuario_id, empresa_id FROM derived_companies
),
upsert_default AS (
  INSERT INTO usuario_empresa_preferencia (tenant_id, usuario_id, empresa_padrao_id, created_at, updated_at)
  SELECT cd.tenant_id, cd.usuario_id, cd.empresa_id, now(), now()
  FROM candidate_default cd
  WHERE cd.empresa_id IS NOT NULL
    AND NOT EXISTS (
      SELECT 1 FROM usuario_empresa_preferencia uep
      WHERE uep.tenant_id = cd.tenant_id
        AND uep.usuario_id = cd.usuario_id
    )
  RETURNING tenant_id, usuario_id
)
UPDATE usuario_empresa_preferencia uep
SET empresa_padrao_id = cd.empresa_id,
    updated_at = now()
FROM candidate_default cd
WHERE uep.tenant_id = cd.tenant_id
  AND uep.usuario_id = cd.usuario_id
  AND cd.empresa_id IS NOT NULL
  AND (
    NOT EXISTS (
      SELECT 1
      FROM empresa e
      WHERE e.id = uep.empresa_padrao_id
        AND e.tenant_id = uep.tenant_id
    )
    OR EXISTS (
      SELECT 1
      FROM explicit_companies ec
      WHERE ec.tenant_id = uep.tenant_id
        AND ec.usuario_id = uep.usuario_id
        AND NOT EXISTS (
          SELECT 1
          FROM usuario_empresa_acesso uea
          WHERE uea.tenant_id = uep.tenant_id
            AND uea.usuario_id = uep.usuario_id
            AND uea.empresa_id = uep.empresa_padrao_id
        )
    )
  );
