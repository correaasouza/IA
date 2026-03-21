ALTER TABLE entidade_qualificacao_item
  ALTER COLUMN tipo TYPE VARCHAR(1)
  USING NULLIF(BTRIM(tipo), '');
