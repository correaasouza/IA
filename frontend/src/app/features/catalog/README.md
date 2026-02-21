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

## Ajustes de estoque do locatario
- Endpoints:
  - `GET /api/catalog/configuration/{type}/stock-adjustments`
  - `GET /api/catalog/configuration/{type}/stock-adjustments/options`
  - `PUT /api/catalog/configuration/{type}/stock-adjustments`
  - `PUT /api/catalog/configuration/{type}/stock-adjustments/{adjustmentId}`
  - `DELETE /api/catalog/configuration/{type}/stock-adjustments/{adjustmentId}`
- Acesso por botao na tela de `Catalogo > Configuracao` (abas `Produtos` e `Servicos`).
- Lista e formulario separados:
  - lista em modal de tipos de ajuste;
  - formulario em modal dedicado, com `tipo` (`ENTRADA`, `SAIDA`, `TRANSFERENCIA`) e selecao de `estoque origem/destino`.
- `codigo` e gerado automaticamente por locatario (incremental) e exibido apenas no header da ficha.

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

## Unidades de medida e historico
- Cada item de catalogo (produto/servico) deve ter `unidade base` obrigatoria.
- O item pode ter `unidade alternativa` + `fator de conversao`, quando aplicavel.
- Depois da primeira movimentacao de estoque do item, unidade base/alternativa/fator ficam bloqueados para alteracao.
- Itens de movimento de estoque gravam:
  - unidade informada no item;
  - unidade base do catalogo;
  - fator aplicado na conversao;
  - quantidade informada e quantidade convertida para base.
- O historico em `catalog_movement` congela snapshot de unidade e fator no momento do lancamento.
- Regras de conversao retroativas nao recalculam movimentos ja lancados.

## Ficha de item de catalogo
- A ficha (`novo`, `consultar`, `editar`) exibe campo obrigatorio `Unidade` (unidade do locatario).
- A ficha nao possui selecao manual de `Grupo`; o grupo atual fica apenas informativo na ficha.
- Movimentacao entre grupos continua no fluxo de lista/arvore de grupos.

## Endpoints de apoio para unidade em movimentos
- `GET /api/movimentos/MOVIMENTO_ESTOQUE/catalogo-itens/{catalogType}/{catalogItemId}/allowed-units`
- `POST /api/movimentos/MOVIMENTO_ESTOQUE/catalogo-itens/preview-conversion`
- `GET /api/tenant/units`
- `GET /api/tenant/unit-conversions`

## Observacoes
- O CRUD resolve contexto por empresa selecionada no header global.
- O modo de numeracao (`AUTOMATICA` ou `MANUAL`) vem da configuracao por agrupador da empresa.
- Na ficha do agrupador em `Catalogo > Configuracao`, as abas seguem a mesma linha: `Empresas`, `Configuracoes` e `Tipos de Estoque`.
- Padrao de UX: as telas de Produtos e Servicos seguem o mesmo padrao base de Entidades (`EntityRecords`):
  - header compacto com contexto;
  - filtro principal com `field-search`;
  - status no seletor lateral;
  - botao de grupos no header de filtros;
  - arvore de grupos inline retratil;
  - area de movimentacao de grupos (desktop) para mover item ou mover itens de um grupo para outro via drop;
  - sem botao "Gerenciar grupos" na lista (a acao equivalente e feita pela area de movimentacao);
  - tabela desktop + cards mobile com mesma linguagem visual.
