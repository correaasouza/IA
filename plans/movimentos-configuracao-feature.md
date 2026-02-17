# movimentos-configuracao-feature

## Contexto
- O sistema precisa de um modulo unico para configurar regras de movimentos.
- Cada movimento pertence a uma unica empresa do locatario.
- Cada tipo de movimento pode ter multiplas configuracoes, com aplicacao para uma ou mais empresas.
- A escolha da configuracao efetiva precisa ser deterministica e sem conflito.

## Objetivo
- Entregar um MVP extensivel de Configuracao de Movimentos, cobrindo:
  - cadastro e manutencao de configuracoes por tipo de movimento;
  - associacao de configuracao para N empresas;
  - resolucao da configuracao efetiva por tipo + empresa (e opcionalmente contexto);
  - validacoes de dominio no backend e no frontend.

## Escopo
- Em escopo (MVP):
  - tipos de movimento listados no requisito;
  - propriedades da configuracao: tipos de entidade permitidos + tipo padrao;
  - prioridade para resolver configuracao efetiva;
  - tela unica de configuracoes de movimentos;
  - API REST completa para CRUD e resolucao de efetividade.
- Fora de escopo (pos-MVP):
  - workflows operacionais dos movimentos (emissao, aprovacao, etc.);
  - regras fiscais ou contabilizacao automatica;
  - motor de regras avancado por multiplos eixos (canal, filial operacional, etc.) alem do contexto opcional planejado.

## 1) Modelagem e decisoes de arquitetura

### 1.1 Enum de tipo de movimento
Criar enum unico no backend e model correspondente no frontend:

```java
public enum MovimentoTipo {
  MOVIMENTO_ESTOQUE,
  SOLICITACAO_ORCAMENTO_COMPRA,
  COTACAO_COMPRA,
  ORCAMENTO_VENDA,
  ORCAMENTO_VEICULAR,
  ORCAMENTO_EQUIPAMENTO,
  ORDEM_COMPRA,
  PEDIDO_VENDA,
  PEDIDO_VEICULAR,
  PEDIDO_EQUIPAMENTO,
  CONTRATO_LOCACAO,
  NOTA_FISCAL_ENTRADA,
  NOTA_FISCAL_SAIDA
}
```

### 1.2 Classe generica de movimento
Criar base abstrata para todos os movimentos:

- `MovimentoBase` (classe abstrata / `@MappedSuperclass`):
  - `id`
  - `tenantId`
  - `empresaId` (obrigatorio, FK para `empresa`)
  - `tipoMovimento` (`MovimentoTipo`)
  - `dataMovimento`
  - `status` (se aplicavel)
  - auditoria (`createdAt`, `updatedAt`, etc)

Regra: qualquer entidade concreta de movimento deve estender `MovimentoBase`.

### 1.3 Entidades de configuracao

#### a) `movimento_config`
- Campos principais:
  - `id`
  - `tenant_id` (not null)
  - `movimento_tipo` (enum/string)
  - `nome` (identificacao funcional)
  - `descricao` (opcional)
  - `prioridade` (int, default 100)
  - `contexto_key` (varchar, opcional para segmentacao futura)
  - `ativo` (boolean)
  - `version` (optimistic lock)
  - auditoria

#### b) `movimento_config_empresa`
- Relacao N:N configuracao x empresas
- Campos:
  - `id`
  - `tenant_id`
  - `movimento_config_id` (FK)
  - `empresa_id` (FK)
  - auditoria
- Constraint:
  - `unique(tenant_id, movimento_config_id, empresa_id)`

#### c) `movimento_config_tipo_entidade`
- Tipos de entidade permitidos por configuracao
- Campos:
  - `id`
  - `tenant_id`
  - `movimento_config_id` (FK)
  - `tipo_entidade_id` (FK `tipo_entidade`)
- Constraint:
  - `unique(tenant_id, movimento_config_id, tipo_entidade_id)`

#### d) Campo de tipo padrao
- Em `movimento_config`:
  - `tipo_entidade_padrao_id` (FK `tipo_entidade`, nullable no banco, obrigatorio na regra de negocio quando houver permitidos)

### 1.4 Reuso do padrao "configuracao aplicada a N empresas"
- Reusar o padrao visual e de interacao ja adotado no sistema (dual-list/toggle mobile/lista de empresas) hoje representado em `frontend/src/app/features/configs/agrupadores-empresa.component.ts`.
- Extrair/reusar o subcomponente de selecao multiempresa para evitar duplicacao de UX.
- Backend nao precisa obrigatoriamente usar `agrupador_empresa`; pode manter join direta `movimento_config_empresa` para garantir query e constraints mais simples.

### 1.5 Regra de escolha da configuracao efetiva (deterministica)
Entrada:
- `tenantId`
- `movimentoTipo`
- `empresaId`
- `contextoKey` opcional

Filtro:
- `ativo = true`
- `movimento_tipo = input`
- empresa presente em `movimento_config_empresa`
- contexto:
  - se `contextoKey` informado: aceitar configs com `contexto_key = contextoKey` ou `contexto_key is null`
  - se nao informado: aceitar apenas `contexto_key is null`

Ordenacao (determinismo):
1. `prioridade desc` (maior vence)
2. especificidade de contexto (`contexto_key not null` vence `null`)
3. `updated_at desc`
4. `id desc`

Pseudocodigo:

```text
candidatos = buscarConfigsAtivas(tenant, tipo, empresa, contexto)
if candidatos.vazio -> sem configuracao aplicavel
ordenar(candidatos,
  prioridade desc,
  contextoEspecifico desc,
  updatedAt desc,
  id desc)
if empateCriticoMesmoPeso(candidatos[0], candidatos[1]) -> erro de conflito
return candidatos[0]
```

### 1.6 Tratamento de conflitos
- Conflito impeditivo no MVP:
  - duas configs ativas, mesmo `movimento_tipo`, mesma empresa, mesmo `contexto_key`, mesma `prioridade`.
- Regra ao salvar:
  - validar sobreposicao e bloquear persistencia com erro de dominio claro.
- Opcional hardening:
  - lock transacional (`PESSIMISTIC_WRITE`) no conjunto impactado durante create/update para evitar race.

### 1.7 Regra de consistencia
- `tipo_entidade_padrao_id` deve estar contido nos permitidos.
- Se remover tipo permitido que e o padrao:
  - bloquear operacao com mensagem "padrao deve ser alterado antes da remocao".
- Se remover empresa da configuracao:
  - configuracao deixa de ser candidata para essa empresa imediatamente.
- Config inativa:
  - nao entra na resolucao efetiva;
  - manter historico (soft delete / arquivamento).

### 1.8 Sem configuracao aplicavel
Politica recomendada no MVP:
- bloquear uso do movimento e retornar erro funcional: `movimento_config_nao_encontrada`.
- mensagem orientativa: "Configure o tipo de movimento para a empresa atual em Configuracoes de Movimentos".

## 2) Backend (Spring Boot)

### 2.1 Pacotes e classes sugeridas
- `domain`:
  - `MovimentoTipo`
  - `MovimentoConfig`
  - `MovimentoConfigEmpresa`
  - `MovimentoConfigTipoEntidade`
  - `MovimentoBase` (abstrata)
- `repository`:
  - `MovimentoConfigRepository`
  - `MovimentoConfigEmpresaRepository`
  - `MovimentoConfigTipoEntidadeRepository`
- `service`:
  - `MovimentoConfigService` (CRUD + validacoes)
  - `MovimentoConfigResolverService` (resolucao efetiva)
- `web`:
  - `MovimentoConfigController`
  - `MovimentoConfigResolverController`

### 2.2 Endpoints REST

#### Tipos disponiveis
- `GET /api/movimentos/configuracoes/tipos`

Resposta exemplo:

```json
[
  { "codigo": "MOVIMENTO_ESTOQUE", "descricao": "Movimento de Estoque" },
  { "codigo": "ORDEM_COMPRA", "descricao": "Ordem de Compra" }
]
```

#### Listar configuracoes por tipo
- `GET /api/movimentos/configuracoes?tipo=ORDEM_COMPRA&page=0&size=20`

#### Obter por id
- `GET /api/movimentos/configuracoes/{id}`

#### Criar
- `POST /api/movimentos/configuracoes`

Payload exemplo:

```json
{
  "tipoMovimento": "ORDEM_COMPRA",
  "nome": "OC padrao revenda",
  "descricao": "Configuracao principal",
  "prioridade": 200,
  "contextoKey": null,
  "ativo": true,
  "empresaIds": [101, 102, 103],
  "tiposEntidadePermitidos": [10, 20, 30],
  "tipoEntidadePadraoId": 20
}
```

#### Atualizar
- `PUT /api/movimentos/configuracoes/{id}`

#### Duplicar (recomendado)
- `POST /api/movimentos/configuracoes/{id}/duplicar`

Payload opcional:

```json
{
  "nome": "OC padrao revenda - copia",
  "prioridade": 150,
  "ativo": false
}
```

#### Excluir (soft delete)
- `DELETE /api/movimentos/configuracoes/{id}`

#### Resolver configuracao efetiva
- `GET /api/movimentos/configuracoes/resolver?tipo=ORDEM_COMPRA&empresaId=101&contextoKey=REVENDA`

Resposta exemplo:

```json
{
  "configuracaoId": 999,
  "tipoMovimento": "ORDEM_COMPRA",
  "empresaId": 101,
  "contextoKey": "REVENDA",
  "prioridade": 200,
  "tipoEntidadePadraoId": 20,
  "tiposEntidadePermitidos": [10, 20, 30]
}
```

### 2.3 Validacoes de dominio
- Bean Validation:
  - `tipoMovimento` obrigatorio
  - `nome` obrigatorio e tamanho maximo
  - `prioridade` >= 0
  - `empresaIds` nao vazio
  - `tiposEntidadePermitidos` nao vazio
  - `tipoEntidadePadraoId` obrigatorio
- Validacoes de servico:
  - padrao contido nos permitidos
  - empresas pertencem ao tenant
  - tipos de entidade pertencem ao tenant
  - conflito por prioridade/contexto/empresa

### 2.4 Multi-tenant e seguranca
- Tenant sempre por `TenantContext`.
- Todas as queries filtradas por `tenant_id`.
- Nao confiar em `tenant_id` do payload.
- Autorizacao:
  - leitura: role equivalente a configuracao (ex.: `CONFIG_EDITOR` ou permissao dedicada)
  - escrita: permissao administrativa de configuracao.
- Auditoria:
  - registrar create/update/delete/duplicate/resolve-failure.

### 2.5 Migracao de banco (Flyway)
Criar migration em `backend/src/main/resources/db/migration` com:
- `movimento_config`
- `movimento_config_empresa`
- `movimento_config_tipo_entidade`
- FKs, indices, unique constraints

Indices recomendados:
- `idx_mov_cfg_tenant_tipo_ativo`
- `idx_mov_cfg_empresa_lookup (tenant_id, empresa_id)` via join table
- `idx_mov_cfg_contexto_prioridade`

Soft delete:
- usar `ativo=false` + manter registro.

### 2.6 Testes backend
- `MovimentoConfigServiceTest`
  - cria configuracao valida
  - bloqueia padrao fora de permitidos
  - bloqueia conflito de prioridade para mesma empresa/tipo/contexto
- `MovimentoConfigResolverServiceTest`
  - prioridade maior vence
  - contexto especifico vence fallback
  - desempate por `updatedAt` e `id`
  - sem configuracao -> erro funcional
- `MovimentoConfigTenantIsolationTest`
  - dados de tenant A invisiveis para tenant B

## 3) Frontend (Angular)

### 3.1 Tela unica
- Nova rota: `/movement-configs` (ou `/configs/movimentos`, alinhado ao menu atual).
- Estrutura da pagina seguindo padroes AGENTS:
  - bloco superior sticky `page-list-sticky`
  - header com titulo/acoes
  - card de filtros no topo
  - mobile com `mobile-filter-toggle`

### 3.2 UX por tipo de movimento
- Uma tela unica com abas/sections por tipo de movimento.
- Dentro de cada aba:
  - lista de configuracoes (table desktop + cards mobile)
  - acoes por item: consultar/alterar/duplicar/excluir
  - acao global: nova configuracao

### 3.3 Formulario de configuracao
Campos:
- nome, descricao, ativo, prioridade, contextoKey (opcional)
- empresas alvo (reuso do padrao multiempresa existente)
- tipos de entidade permitidos (multi-select)
- tipo de entidade padrao (single, filtrado por permitidos)

Regras de UI:
- ao remover tipo permitido que e padrao, bloquear e orientar troca de padrao
- desabilitar salvar enquanto invalido
- mensagens claras de erro de conflito retornadas do backend

### 3.4 Componentizacao sugerida
- `movement-config-page.component.*` (tela unica)
- `movement-config-list.component.*` (lista por tipo)
- `movement-config-form.component.*` (create/edit/view)
- `movement-config.service.ts`
- `models/movement-config.model.ts`

### 3.5 Resolver configuracao efetiva no frontend
- Endpoint de resolver consumido por fluxos de criacao de movimento.
- Em caso de falha por ausencia de config:
  - toast + redirect contextual para tela de configuracao (se usuario tiver permissao).

## 4) Estrategia de rollout / migracao

### 4.1 Introducao sem quebra
- Feature flag backend/frontend: `movementConfigEnabled`.
- Fluxos atuais continuam operando ate habilitar validacao estrita.

### 4.2 Fases
1. Entregar schema + API + tela (sem bloquear movimentos antigos).
2. Habilitar warnings quando nao houver config aplicavel.
3. Ativar bloqueio obrigatorio apos janela de configuracao do cliente.

### 4.3 Seed inicial (opcoes)
- Opcao A (recomendada para onboarding):
  - criar 1 config default por tipo, inativa e sem empresas (usuario completa).
- Opcao B (estrita):
  - sem seed; obrigar configuracao manual antes de uso.

### 4.4 Checklist de QA
- criar, editar, duplicar, excluir por tipo
- validar conflito de prioridade/contexto/empresa
- validar padrao dentro dos permitidos
- validar lista de empresas aplicada
- validar resolucao efetiva com e sem contexto
- validar isolamento por tenant
- validar responsividade (desktop/mobile) e padrao de filtros mobile

## 5) Criterios de aceite
- [x] Consigo criar multiplas configuracoes por tipo de movimento.
- [x] Para uma empresa + tipo (+contexto), a configuracao efetiva e resolvida de forma deterministica.
- [x] Conflitos sao bloqueados ou resolvidos por prioridade com regra explicita.
- [x] Validacoes de permitidos/padrao funcionam no frontend e no backend.
- [x] Isolamento por locatario garantido em todas as APIs.
- [x] Tela unica de configuracoes de movimentos com seccoes claras por tipo.
- [x] Gestao de empresas alvo por configuracao reutiliza padrao existente do sistema.

## Passos acordados (checklist ordenada por dependencias)
1. [x] Criar enum `MovimentoTipo` compartilhado (backend + frontend model).
2. [x] Criar migration Flyway das tabelas de configuracao.
3. [x] Implementar entidades JPA, repositories e mapeamentos.
4. [x] Implementar servico de dominio com validacoes de conflito e consistencia.
5. [x] Implementar endpoint de resolver configuracao efetiva.
6. [x] Implementar CRUD completo + duplicar + soft delete.
7. [x] Implementar testes de dominio, resolver e isolamento tenant.
8. [x] Criar tela unica Angular com abas por tipo de movimento.
9. [x] Implementar lista por tipo (desktop/mobile) com acoes CRUD.
10. [x] Implementar formulario com empresas alvo + permitidos + padrao.
11. [x] Integrar mensagens de erro de dominio no frontend.
12. [x] Executar QA funcional e de regressao.
13. [x] Definir e aplicar estrategia de rollout (flag + faseamento).
14. [ ] Antes de iniciar implementacao em branch, confirmar com o usuario: branch saindo de `develop` e nome no padrao Jira.

## Criterios de conclusao
- Planejamento validado pelo usuario.
- Escopo MVP fechado e rastreavel em tarefas.
- Regras de determinismo e conflito definidas de ponta a ponta.
- Fluxo de rollout definido para evitar quebra de operacao.
