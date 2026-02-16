# Catalogo - Configuracao e CRUD

## Acesso
- Configuracao: `/catalog/configuration`
- Produtos: `/catalog/products`
- Servicos: `/catalog/services`

## Lazy create (configuracao)
- Ao abrir a configuracao de `PRODUCTS` ou `SERVICES`, o frontend chama `GET /api/catalog/configuration/{type}`.
- Se nao existir registro para o tenant atual, o backend cria automaticamente com:
  - `numberingMode = AUTOMATICA`
  - `active = true`

## Configuracao por Grupo de Empresas
- Endpoints:
  - `GET /api/catalog/configuration/{type}/group-config`
  - `PUT /api/catalog/configuration/{type}/group-config/{agrupadorId}`
- Cada agrupador possui seu proprio `numberingMode`.

## CRUD de catalogo
### Produtos
- Lista: `/catalog/products`
- Novo: `/catalog/products/new`
- Consulta: `/catalog/products/:id`
- Edicao: `/catalog/products/:id/edit`
- Grupos: `/catalog/products/groups`

### Servicos
- Lista: `/catalog/services`
- Novo: `/catalog/services/new`
- Consulta: `/catalog/services/:id`
- Edicao: `/catalog/services/:id/edit`
- Grupos: `/catalog/services/groups`

## Observacoes
- O CRUD resolve contexto por empresa selecionada no header global.
- O modo de numeracao (`AUTOMATICA` ou `MANUAL`) vem da configuracao por agrupador da empresa.
- Padrao de UX: as telas de Produtos e Servicos seguem o mesmo padrao base de Entidades (`EntityRecords`):
  - header compacto com contexto;
  - filtro principal com `field-search`;
  - status no seletor lateral;
  - botao de grupos no header de filtros;
  - arvore de grupos inline retratil;
  - area de movimentacao de grupos (desktop) para mover item ou mover itens de um grupo para outro via drop;
  - sem botao "Gerenciar grupos" na lista (a acao equivalente e feita pela area de movimentacao);
  - tabela desktop + cards mobile com mesma linguagem visual.
