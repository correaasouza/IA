ALTER TABLE pessoa
  ADD COLUMN IF NOT EXISTS tipo_pessoa VARCHAR(20) NOT NULL DEFAULT 'FISICA';

UPDATE pessoa
SET tipo_pessoa = CASE
  WHEN cnpj IS NOT NULL AND cnpj <> '' THEN 'JURIDICA'
  WHEN id_estrangeiro IS NOT NULL AND id_estrangeiro <> '' THEN 'ESTRANGEIRA'
  ELSE 'FISICA'
END;
