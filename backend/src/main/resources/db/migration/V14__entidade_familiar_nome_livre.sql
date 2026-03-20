ALTER TABLE entidade_familiar
  ADD COLUMN IF NOT EXISTS nome VARCHAR(160);

UPDATE entidade_familiar ef
SET nome = COALESCE(p.nome, ef.nome)
FROM registro_entidade re
JOIN pessoa p ON p.id = re.pessoa_id
WHERE ef.entidade_parente_id IS NOT NULL
  AND re.id = ef.entidade_parente_id
  AND re.tenant_id = ef.tenant_id
  AND p.tenant_id = ef.tenant_id;

UPDATE entidade_familiar
SET nome = 'Nao informado'
WHERE nome IS NULL OR btrim(nome) = '';

ALTER TABLE entidade_familiar
  ALTER COLUMN nome SET NOT NULL;

ALTER TABLE entidade_familiar
  ALTER COLUMN entidade_parente_id DROP NOT NULL;

ALTER TABLE entidade_familiar
  DROP CONSTRAINT IF EXISTS fk_ent_familiar_parente_scope;

DROP INDEX IF EXISTS ux_ent_familiar_scope_unique;

CREATE INDEX IF NOT EXISTS idx_ent_familiar_scope_registro_nome
  ON entidade_familiar (tenant_id, empresa_id, registro_entidade_id, nome);
