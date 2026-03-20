ALTER TABLE registro_entidade
  ADD COLUMN IF NOT EXISTS tratamento VARCHAR(120);

UPDATE registro_entidade
SET tratamento = COALESCE(tratamento, tratamento_id::text)
WHERE tratamento IS NULL
  AND tratamento_id IS NOT NULL;
