# Matriz de permissões (Auth Multi-tenant)

| Ação | MASTER global (tenant 1) | ADMIN (tenant) | USER (tenant) |
|---|---|---|---|
| Ver menu Locatários | Sim | Não | Não |
| CRUD Locatários | Sim | Não | Não |
| Listar usuários por tenant (filtro/endpoint) | Sim | Não | Não |
| Listar usuários do tenant ativo | Sim | Sim | Não |
| Criar/editar/desativar/excluir usuário no tenant ativo | Sim | Sim | Não |
| Atribuir acesso de usuário a locatários | Sim | Não | Não |
| Conceder acesso de usuário a empresas | Sim | Sim | Não |
| Ver empresas acessíveis no tenant | Sim | Sim (todas) | Sim (somente vinculadas) |
| Alterar empresa padrão própria | Sim | Sim | Sim (somente empresas acessíveis) |
| Alterar empresa padrão de outro usuário | Sim | Sim | Não |
| Remover papel MASTER do usuário `master` global | Não | Não | Não |
| Criar papéis customizados por locatário | Sim | Sim | Não |

## Regras de escopo
- `X-Tenant-Id` é obrigatório para endpoints tenant-scoped.
- `X-Empresa-Id` é validado no backend contra empresas acessíveis do usuário.
- `ADMIN` tem acesso derivado a todas as empresas do tenant (sem vínculo explícito obrigatório).
- `USER` depende de vínculo explícito em `usuario_empresa_acesso`.
