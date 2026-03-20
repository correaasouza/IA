# Checklist de regressão manual (Auth/Autorização)

## Login e escopo
- Login com `master` no tenant `1`: acessa menus de administração global.
- Login com `admin` de tenant comum: não vê menu Locatários.
- Login com `user` de tenant comum: não vê telas de concessão de acesso.

## Locatários
- `MASTER` global acessa `/tenants` e CRUD.
- `ADMIN/USER` são bloqueados em rotas e endpoints de locatários.

## Usuários
- Lista de usuários abre para `MASTER/ADMIN`.
- Filtro por locatário na lista de usuários aparece só para `MASTER` global.
- Endpoint `/api/tenants/{tenantId}/users` funciona só para `MASTER` global.

## Papéis
- Papéis base (`MASTER`, `ADMIN`, `USER`) existem em todos os locatários.
- É possível criar papéis custom por locatário.
- Tentativa de remover `MASTER` do usuário `master` global falha.

## Empresa e contexto
- `ADMIN` enxerga todas as empresas do tenant.
- `USER` enxerga somente empresas vinculadas.
- `X-Empresa-Id` fora do escopo retorna `403`.
- Se empresa padrão inválida, sistema seleciona a primeira acessível e persiste.

## Concessões de acesso
- Somente `MASTER` global altera acesso por locatário.
- Somente `MASTER/ADMIN` altera acesso por empresa.
- `USER` consegue alterar apenas empresa padrão própria (dentro do escopo).

## Menus e guards (frontend)
- Menu Locatários visível apenas para `MASTER` global.
- Rotas de Locatários com `MasterOnlyGuard`.
- Rotas de concessão por empresa com `AdminOrMasterGuard`.
- Rotas dependentes de empresa ativa com `CompanyScopeGuard`.
