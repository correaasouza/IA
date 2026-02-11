ALTER TABLE entidade_registro
  ADD COLUMN IF NOT EXISTS tipo_pessoa VARCHAR(20) NOT NULL DEFAULT 'FISICA';

UPDATE entidade_registro
SET tipo_pessoa = CASE
  WHEN LENGTH(REGEXP_REPLACE(cpf_cnpj, '\D', '', 'g')) = 14 THEN 'JURIDICA'
  WHEN LENGTH(REGEXP_REPLACE(cpf_cnpj, '\D', '', 'g')) = 11 THEN 'FISICA'
  ELSE 'ESTRANGEIRA'
END;
