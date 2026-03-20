ALTER TABLE usuario_empresa_acesso
  ADD COLUMN IF NOT EXISTS created_by VARCHAR(120),
  ADD COLUMN IF NOT EXISTS updated_by VARCHAR(120);

