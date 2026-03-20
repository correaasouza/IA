ALTER TABLE entidade_contato_forma
  DROP CONSTRAINT IF EXISTS ck_ent_contato_forma_tipo;

ALTER TABLE entidade_contato_forma
  ADD CONSTRAINT ck_ent_contato_forma_tipo
  CHECK (tipo_contato IN (
    'EMAIL',
    'TELEFONE',
    'FONE_CELULAR',
    'FONE_RESIDENCIAL',
    'FONE_COMERCIAL',
    'WHATSAPP',
    'FACEBOOK',
    'SITE',
    'LINKEDIN',
    'INSTAGRAM'
  ));
