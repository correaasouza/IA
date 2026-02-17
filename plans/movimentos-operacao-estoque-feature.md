# Implementacao do Modulo de Movimentos (MVP 1: Movimento de Estoque)

## Resumo
Implementar a operacao de movimentos com base generica (`MovimentoBase`) e primeira concretizacao (`MovimentoEstoque`), com fluxo de pre-carga por configuracao, tela de ficha para completar dados e persistencia final.

## Objetivo funcional
1. Todo movimento pertence a uma empresa do contexto.
2. Backend fornece endpoint de template que resolve configuracao aplicavel e devolve defaults.
3. Front abre ficha com defaults e usuario preenche os demais dados.
4. Primeiro movimento implementado: `MOVIMENTO_ESTOQUE` com atributo obrigatorio `nome`.
5. Nao havera workflow/status nesta etapa.
6. Uso sem configuracao aplicavel deve bloquear operacao.

## Escopo MVP
1. Backend: entidade concreta, CRUD + template para `MOVIMENTO_ESTOQUE`.
2. Frontend: lista e ficha separadas para movimento de estoque.
3. Seguranca: permissao nova `MOVIMENTO_ESTOQUE_OPERAR`.
4. Integracao com menu dinamico ja existente.
5. Reuso total dos padroes AGENTS.md.

## Fora de escopo MVP
1. Workflow de estados de movimento.
2. Efeitos contabeis/fiscais.
3. Regras especificas dos outros tipos de movimento.
4. Lancamento operacional de impactos de saldo nesta primeira ficha.

## Decisoes fechadas
1. Bootstrap: so pre-carga (nao persiste ao abrir ficha).
2. Entrada pelo menu: abrir lista.
3. Empresa do movimento: validar `empresaId` do payload contra `X-Empresa-Id`.
4. Permissao operacional: `MOVIMENTO_ESTOQUE_OPERAR`.
5. Sem campo status por enquanto.
6. Nome da aba em padroes relacionados: `Empresas`.

## Arquitetura backend

### 1) Dominio e persistencia
1. Manter `MovimentoBase` como classe generica.
2. Criar entidade `MovimentoEstoque` em `backend/src/main/java/com/ia/app/domain/MovimentoEstoque.java`.
3. Campos de `MovimentoEstoque`: `id`, `tenantId`, `empresaId`, `tipoMovimento`, `dataMovimento`, `nome`, `movimentoConfigId`, `tipoEntidadePadraoId`, `version`, auditoria.
4. Criar migration `V16__movimento_estoque.sql` com tabela `movimento_estoque`, FKs por tenant (`empresa`, `movimento_config`, `tipo_entidade`), indices por tenant/empresa/data e tenant/nome.
5. Nao remover `status` de `MovimentoBase` nesta etapa.

### 2) Camada de operacao por tipo
1. Criar contrato `MovimentoOperacaoHandler`: `supports()`, `buildTemplate(...)`, `create(...)`, `list(...)`, `get(...)`, `update(...)`, `delete(...)`.
2. Criar implementacao `MovimentoEstoqueOperacaoHandler`.
3. Criar fachada `MovimentoOperacaoService` para roteamento por `MovimentoTipo`.
4. Reusar `MovimentoConfigService.resolve(...)` para pre-carga e validacao no salvar.

### 3) DTOs e contratos publicos
1. `MovimentoTemplateRequest`: `{ "empresaId": 3 }`
2. `MovimentoEstoqueTemplateResponse`:
```json
{
  "tipoMovimento": "MOVIMENTO_ESTOQUE",
  "empresaId": 3,
  "movimentoConfigId": 12,
  "tipoEntidadePadraoId": 7,
  "tiposEntidadePermitidos": [7, 8],
  "nome": "",
  "dataMovimento": "2026-02-17"
}
```
3. `MovimentoEstoqueCreateRequest`: `{ "empresaId": 3, "nome": "Movimento de ajuste 31/12/2025", "dataMovimento": "2025-12-31" }`
4. `MovimentoEstoqueUpdateRequest`: `{ "empresaId": 3, "nome": "Movimento de ajuste 31/12/2025 - revisado", "dataMovimento": "2025-12-31", "version": 0 }`
5. `MovimentoEstoqueResponse`:
```json
{
  "id": 101,
  "tipoMovimento": "MOVIMENTO_ESTOQUE",
  "empresaId": 3,
  "nome": "Movimento de ajuste 31/12/2025",
  "dataMovimento": "2025-12-31",
  "movimentoConfigId": 12,
  "tipoEntidadePadraoId": 7,
  "version": 0
}
```

### 4) Endpoints REST
1. `POST /api/movimentos/{tipo}/template`
2. `POST /api/movimentos/{tipo}`
3. `GET /api/movimentos/{tipo}`
4. `GET /api/movimentos/{tipo}/{id}`
5. `PUT /api/movimentos/{tipo}/{id}`
6. `DELETE /api/movimentos/{tipo}/{id}`

Regras:
1. MVP suporta apenas `MOVIMENTO_ESTOQUE`; demais retornam `movimento_tipo_nao_implementado`.
2. `empresaId` obrigatorio no body e igual ao header `X-Empresa-Id`.
3. `nome` obrigatorio e max 120.
4. Sem configuracao aplicavel: `movimento_config_nao_encontrada`.
5. Isolamento por tenant em todas as queries.

### 5) Seguranca e permissoes
1. Incluir `MOVIMENTO_ESTOQUE_OPERAR` em `PermissaoCodigo`, `PermissaoCatalogService.seedDefaults`, `PapelSeedService.seedDefaults`.
2. Migration seed tenant 1: `V17__permissao_movimento_estoque_operar.sql`.
3. Controllers de operacao usam `@PreAuthorize("@permissaoGuard.hasPermissao('MOVIMENTO_ESTOQUE_OPERAR')")`.
4. Configuracao de movimentos continua com `CONFIG_EDITOR`.

### 6) Tratamento de erros
Adicionar no `ApiExceptionHandler`:
1. `movimento_empresa_id_required`
2. `movimento_empresa_context_required`
3. `movimento_empresa_context_mismatch`
4. `movimento_estoque_nome_required`
5. `movimento_estoque_not_found`
6. `movimento_tipo_nao_implementado`

## Arquitetura frontend

### 1) Rotas (lista e ficha separadas)
1. `/movimentos/estoque`
2. `/movimentos/estoque/new`
3. `/movimentos/estoque/:id`
4. `/movimentos/estoque/:id/edit`

### 2) Servicos
1. Criar `frontend/src/app/features/movements/movement-operation.service.ts`.
2. Metodos: `buildTemplate`, `listEstoque`, `getEstoque`, `createEstoque`, `updateEstoque`, `deleteEstoque`.

### 3) Componentes
1. Lista: `movimento-estoque-list.component.ts/html/css`
2. Ficha: `movimento-estoque-form.component.ts/html/css`

### 4) Padroes AGENTS.md obrigatorios
1. Lista e ficha em telas separadas.
2. Lista com `page-list-sticky` e filtros no topo.
3. Coluna de acoes fixa com `stickyEnd` + `.table-sticky-actions`.
4. Ficha com `page-header-sticky` + `Nome: <valor atual>`.
5. Rotas dedicadas novo/consultar/editar.
6. `appAccessControl`: `movimentos.estoque.create`, `movimentos.estoque.update`, `movimentos.estoque.delete`, `menu.movement.action.movimento_estoque`.
7. Datas no padrao global (`DD/MM/AAAA`, `appDateMask`, `toIsoDate`, `toDisplayDate`).

### 5) Menu dinamico
1. Em `app.component.ts`, mapear `MOVIMENTO_ESTOQUE` para `/movimentos/estoque`.
2. Aplicar permissao `MOVIMENTO_ESTOQUE_OPERAR`.
3. Tipos nao implementados sem rota funcional nesta fase.

### 6) Fluxo UX
1. Usuario abre menu "Movimento de Estoque" e cai na lista.
2. Clica em "Novo movimento".
3. Front chama `POST /template` com `empresaId` atual.
4. Template ok abre ficha com defaults.
5. Usuario informa `nome` e salva.
6. Backend persiste e retorna registro.
7. Front redireciona para consultar/editar.

## Fases de implementacao
1. Base backend: migration, entidade/repository, DTOs, handler/fachada/controller, erros e seguranca.
2. UI frontend: service, lista/ficha, rotas, `appAccessControl`, menu dinamico.
3. Qualidade: testes, QA manual, ajustes de mensagem e acessibilidade.

## Checklist
1. Salvar plano em `plans/movimentos-operacao-estoque-feature.md`.
2. Criar `V16__movimento_estoque.sql`.
3. Criar entidade/repository `MovimentoEstoque`.
4. Implementar DTOs + handler/fachada.
5. Implementar controller `/api/movimentos/{tipo}`.
6. Adicionar permissao `MOVIMENTO_ESTOQUE_OPERAR` + seed `V17`.
7. Ajustar `ApiExceptionHandler`.
8. Criar service Angular de operacao.
9. Criar lista de movimento de estoque.
10. Criar ficha de movimento de estoque.
11. Configurar rotas.
12. Integrar menu dinamico para rota real do estoque.
13. Implementar testes automatizados.
14. Executar build/test e QA manual final.
