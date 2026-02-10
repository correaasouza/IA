ALTER TABLE entidade
  RENAME COLUMN observacao TO alerta;

ALTER TABLE entidade
  DROP COLUMN IF EXISTS codigo_externo;
