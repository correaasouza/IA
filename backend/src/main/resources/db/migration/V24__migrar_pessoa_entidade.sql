WITH origem AS (
  SELECT
    er.id,
    er.tenant_id,
    er.entidade_definicao_id,
    er.nome,
    er.apelido,
    er.ativo,
    REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') AS doc_digits,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) = 11
      THEN REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') END AS cpf,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) = 14
      THEN REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') END AS cnpj,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) NOT IN (11, 14)
      THEN er.cpf_cnpj END AS id_estrangeiro
  FROM entidade_registro er
)
INSERT INTO pessoa (tenant_id, nome, apelido, cpf, cnpj, id_estrangeiro, ativo, versao, created_at, updated_at)
SELECT DISTINCT
  tenant_id,
  nome,
  apelido,
  cpf,
  cnpj,
  id_estrangeiro,
  TRUE,
  1,
  NOW(),
  NOW()
FROM origem
ON CONFLICT DO NOTHING;

WITH origem AS (
  SELECT
    er.id,
    er.tenant_id,
    er.entidade_definicao_id,
    er.nome,
    er.apelido,
    er.ativo,
    REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') AS doc_digits,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) = 11
      THEN REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') END AS cpf,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) = 14
      THEN REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') END AS cnpj,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) NOT IN (11, 14)
      THEN er.cpf_cnpj END AS id_estrangeiro
  FROM entidade_registro er
)
INSERT INTO entidade (tenant_id, tipo_entidade_id, pessoa_id, ativo, versao, created_at, updated_at)
SELECT
  o.tenant_id,
  te.id,
  p.id,
  o.ativo,
  1,
  NOW(),
  NOW()
FROM origem o
JOIN entidade_definicao ed ON ed.id = o.entidade_definicao_id
JOIN tipo_entidade te ON te.tenant_id = o.tenant_id AND te.codigo = ed.codigo
JOIN pessoa p ON p.tenant_id = o.tenant_id AND (
  (o.cpf IS NOT NULL AND p.cpf = o.cpf) OR
  (o.cnpj IS NOT NULL AND p.cnpj = o.cnpj) OR
  (o.id_estrangeiro IS NOT NULL AND p.id_estrangeiro = o.id_estrangeiro)
);

WITH origem AS (
  SELECT
    er.id,
    er.tenant_id,
    REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') AS doc_digits,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) = 11
      THEN REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') END AS cpf,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) = 14
      THEN REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g') END AS cnpj,
    CASE WHEN LENGTH(REGEXP_REPLACE(er.cpf_cnpj, '\D', '', 'g')) NOT IN (11, 14)
      THEN er.cpf_cnpj END AS id_estrangeiro
  FROM entidade_registro er
)
INSERT INTO pessoa_contato (tenant_id, pessoa_id, tipo, valor, principal, created_at, updated_at)
SELECT
  c.tenant_id,
  p.id,
  c.tipo,
  c.valor,
  c.principal,
  c.created_at,
  c.updated_at
FROM contato c
JOIN entidade_registro er ON er.id = c.entidade_registro_id
JOIN origem o ON o.id = er.id
JOIN pessoa p ON p.tenant_id = o.tenant_id AND (
  (o.cpf IS NOT NULL AND p.cpf = o.cpf) OR
  (o.cnpj IS NOT NULL AND p.cnpj = o.cnpj) OR
  (o.id_estrangeiro IS NOT NULL AND p.id_estrangeiro = o.id_estrangeiro)
);
