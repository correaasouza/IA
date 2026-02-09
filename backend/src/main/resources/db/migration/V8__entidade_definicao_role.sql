ALTER TABLE entidade_definicao
  ADD COLUMN IF NOT EXISTS role_required VARCHAR(120);
