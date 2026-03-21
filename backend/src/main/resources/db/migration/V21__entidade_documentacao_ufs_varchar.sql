ALTER TABLE entidade_documentacao
  ALTER COLUMN rg_uf_emissao TYPE VARCHAR(2)
  USING NULLIF(BTRIM(rg_uf_emissao), '');

ALTER TABLE entidade_documentacao
  ALTER COLUMN registro_estadual_uf TYPE VARCHAR(2)
  USING NULLIF(BTRIM(registro_estadual_uf), '');

ALTER TABLE entidade_endereco
  ALTER COLUMN uf TYPE VARCHAR(2)
  USING NULLIF(BTRIM(uf), '');
