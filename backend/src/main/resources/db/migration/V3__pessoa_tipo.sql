<<<<<<< HEAD
ALTER TABLE pessoa
  ADD COLUMN IF NOT EXISTS tipo_pessoa VARCHAR(20) NOT NULL DEFAULT 'FISICA';

UPDATE pessoa
SET tipo_pessoa = CASE
  WHEN cnpj IS NOT NULL AND cnpj <> '' THEN 'JURIDICA'
  WHEN id_estrangeiro IS NOT NULL AND id_estrangeiro <> '' THEN 'ESTRANGEIRA'
  ELSE 'FISICA'
END;
=======
-- no-op apos reset de baseline consolidado em V1
SELECT 1;
>>>>>>> 4cd7063 (refactor(db): consolidar baseline e resetar migracoes V2-V5)
