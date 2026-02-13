<<<<<<< HEAD
UPDATE permissao_catalogo
SET label = 'Admin do Locatário'
WHERE codigo = 'TENANT_ADMIN' AND label LIKE '%LocatÃ%';

UPDATE permissao_catalogo
SET label = 'Configurar colunas e formulários'
WHERE codigo = 'CONFIG_EDITOR' AND label LIKE '%formulÃ%';

UPDATE permissao_catalogo
SET label = 'Gerenciar usuários'
WHERE codigo = 'USUARIO_MANAGE' AND label LIKE '%usuÃ%';

UPDATE permissao_catalogo
SET label = 'Gerenciar papéis'
WHERE codigo = 'PAPEL_MANAGE' AND label LIKE '%papÃ%';

UPDATE permissao_catalogo
SET label = 'Visualizar relatórios'
WHERE codigo = 'RELATORIO_VIEW' AND label LIKE '%relatÃ%';

UPDATE papel
SET descricao = 'Administrador do locatário'
WHERE nome = 'ADMIN' AND descricao LIKE '%locatÃ%';

UPDATE papel
SET descricao = 'Usuário padrão'
WHERE nome = 'USUARIO' AND descricao LIKE '%UsuÃ%';

UPDATE tipo_entidade
SET nome = 'Funcionário'
WHERE codigo = 'FUNCIONARIO' AND nome LIKE '%FuncionÃ%';

UPDATE entidade_definicao
SET nome = 'Funcionário'
WHERE codigo = 'FUNCIONARIO' AND nome LIKE '%FuncionÃ%';
=======
-- no-op apos reset de baseline consolidado em V1
SELECT 1;
>>>>>>> 4cd7063 (refactor(db): consolidar baseline e resetar migracoes V2-V5)
