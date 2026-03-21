ALTER TABLE entidade_documentacao
  ALTER COLUMN ctps_uf_emissao TYPE VARCHAR(2)
  USING NULLIF(BTRIM(ctps_uf_emissao), '');
