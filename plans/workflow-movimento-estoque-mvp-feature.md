# workflow-movimento-estoque-mvp-feature
## Contexto
O ERP multi-tenant ja possui catalogo, movimentos e itens de movimento de estoque. Foi definido um modulo de workflow proprio (sem BPMN externo), versionado e reutilizavel, com primeira integracao apenas nas origens `MOVIMENTO_ESTOQUE` e `ITEM_MOVIMENTO_ESTOQUE`.

## Objetivo
Entregar o MVP do modulo de workflow para controlar estados/transicoes dessas duas origens, com auditoria e acao automatica `MOVE_STOCK` executada no workflow de item de movimento.

## Escopo
1. Backend workflow generico com definicoes versionadas (draft/published), instancias por registro, historico e execucao de acoes.
2. API REST para CRUD de definicao, validacao, publicacao, clone, import/export JSON, runtime de estado/transicao e historico.
3. Integracao de dominio:
   - `MovimentoEstoque` com `stockAdjustmentId` e sincronizacao de status.
   - `MovimentoEstoqueItem` com status e marcadores de idempotencia de movimentacao.
4. Acao `MOVE_STOCK` no item:
   - le item + movimento pai;
   - le tipo de ajuste no cabecalho do movimento;
   - aplica `CatalogMovementEngine`;
   - garante idempotencia (marca no item + execution key);
   - registra auditoria.
5. Frontend:
   - modulo de workflow (lista/ficha separadas) com builder visual;
   - configuracao de estados/transicoes/acoes;
   - publicar, clonar, exportar e importar JSON.
6. Integracao UI de movimentos:
   - campo de ajuste de estoque na ficha;
   - exibicao de estado dos itens;
   - execucao de transicao de item na lista e na ficha.

## Passos acordados
1. Criar migracao com tabelas `workflow_definition`, `workflow_state`, `workflow_transition`, `workflow_instance`, `workflow_history`, `workflow_action_execution`, indices e colunas novas em movimento/item.
2. Implementar entidades/repositorios/services/controllers do modulo `com.ia.app.workflow`.
3. Implementar engine de transicao com validacoes de permissao/condicao e execucao de acoes.
4. Implementar `MoveStockAction` com lock, idempotencia e logs.
5. Integrar inicializacao de instancias no fluxo de criacao/atualizacao de movimento e itens.
6. Adicionar permissoes `WORKFLOW_CONFIGURAR` e `WORKFLOW_TRANSICIONAR`, feature flag `workflow.enabled` e exposicao em `/api/me`.
7. Criar frontend `features/workflows` com lista/ficha/builder e rotas dedicadas.
8. Integrar transicoes de workflow na UI de itens de movimento.
9. Validar com build backend/frontend e testes backend.

## Critérios de conclusão
1. Workflows de `MOVIMENTO_ESTOQUE` e `ITEM_MOVIMENTO_ESTOQUE` configuraveis via UI e persistidos por tenant.
2. Publicacao de definicao cria versao imutavel e runtime usa definicao publicada.
3. Transicao de item executa `MOVE_STOCK` com idempotencia e auditoria.
4. Leitura de estado atual e historico funcionando via endpoints runtime.
5. Ficha/lista de movimento com ajuste de estoque + estado/transicao de itens funcionando.
6. Build `mvn test` e `npm run build` sem erros.
