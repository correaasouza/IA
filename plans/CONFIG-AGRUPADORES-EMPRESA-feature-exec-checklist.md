# Execucao Tecnica - Agrupadores de Empresas por Configuracao

## Objetivo
- Transformar o plano `plans/CONFIG-AGRUPADORES-EMPRESA-feature.md` em backlog implementavel, com ordem de execucao e criterios de conclusao por item.

---

## Status Atual (Execucao)
- [x] Sprint 1 (DB + Backend Core) implementada
- [x] Sprint 2 (Frontend Reutilizavel + UX base) implementada
- [x] Sprint 3 (Hardening) com backend concluido
- [x] E2E base Playwright do fluxo principal de conflito
- [x] Pendencias para fechar 100% do plano original (nenhuma pendencia aberta)
- [x] Testes unitarios frontend (servico/componente)
- [x] E2E adicional: remover agrupador e reaproveitar empresa
- [x] Revisao final de permissoes por `configType`

---

## Sprint 1 - Fundacoes (DB + Backend Core)

## 1) Banco de dados (Flyway)
- [x] Criar migration `backend/src/main/resources/db/migration/V7__agrupador_empresa_por_configuracao.sql`
- [x] Criar tabela `agrupador_empresa`
- [x] `id`, `tenant_id`, `config_type`, `config_id`, `nome`, `ativo`, auditoria
- [x] Criar tabela `agrupador_empresa_item`
- [x] `id`, `tenant_id`, `agrupador_id`, `config_type`, `config_id`, `empresa_id`, auditoria
- [x] Criar constraints obrigatorias
- [x] UNIQUE nome por configuracao (`tenant_id`, `config_type`, `config_id`, `lower(nome)`)
- [x] UNIQUE empresa por configuracao (`tenant_id`, `config_type`, `config_id`, `empresa_id`)
- [x] FK item -> agrupador (com tenant e config)
- [x] FK item -> empresa (tenant + empresa)
- [x] Criar indices de performance
- [x] `idx_agrupador_cfg`
- [x] `idx_item_agrupador`
- [x] `idx_item_cfg_empresa`
- [x] `idx_item_empresa`
- [x] Validar se existe indice unico em `empresa(tenant_id, id)`; criar se necessario

## 2) Dominio e persistencia (Spring)
- [x] Criar entidade `AgrupadorEmpresa` (`backend/src/main/java/com/ia/app/domain/`)
- [x] Criar entidade `AgrupadorEmpresaItem` (`backend/src/main/java/com/ia/app/domain/`)
- [x] Criar repositorios:
- [x] `AgrupadorEmpresaRepository`
- [x] `AgrupadorEmpresaItemRepository`
- [x] Criar DTOs request/response para:
- [x] criar agrupador
- [x] editar nome
- [x] adicionar empresa
- [x] detalhe/listagem de agrupador com empresas

## 3) Servico de negocio
- [x] Criar `AgrupadorEmpresaService`
- [x] Implementar validacoes:
- [x] tenant atual obrigatorio (`TenantContext`)
- [x] configType/configId valido
- [x] empresa pertence ao tenant
- [x] agrupador pertence ao tenant e a configuracao
- [x] nome unico por configuracao
- [x] Implementar operacoes:
- [x] listagem por configuracao
- [x] detalhe
- [x] criar agrupador
- [x] editar nome
- [x] adicionar empresa
- [x] remover empresa
- [x] remover agrupador (hard delete)

## 4) Concorrencia e conflito
- [x] Marcar `adicionar empresa ao agrupador` como `@Transactional`
- [x] Inserir vinculo sem pre-lock pessimista
- [x] Capturar `DataIntegrityViolationException` da unique de empresa por configuracao
- [x] Traduzir para erro de negocio: `agrupador_empresa_empresa_duplicada_config`
- [x] Responder `409 Conflict` com mensagem clara

## 5) API REST
- [x] Criar controller `AgrupadorEmpresaController`
- [x] Expor endpoints:
- [x] `GET /api/configuracoes/{configType}/{configId}/agrupadores-empresa`
- [x] `GET /api/configuracoes/{configType}/{configId}/agrupadores-empresa/{agrupadorId}`
- [x] `POST /api/configuracoes/{configType}/{configId}/agrupadores-empresa`
- [x] `PATCH /api/configuracoes/{configType}/{configId}/agrupadores-empresa/{agrupadorId}`
- [x] `POST /api/configuracoes/{configType}/{configId}/agrupadores-empresa/{agrupadorId}/empresas`
- [x] `DELETE /api/configuracoes/{configType}/{configId}/agrupadores-empresa/{agrupadorId}/empresas/{empresaId}`
- [x] `DELETE /api/configuracoes/{configType}/{configId}/agrupadores-empresa/{agrupadorId}`
- [x] Aplicar seguranca com permissoes adequadas (`CONFIG_EDITOR`)

## 6) Tratamento de erro padronizado
- [x] Atualizar `backend/src/main/java/com/ia/app/web/ApiExceptionHandler.java`
- [x] Mapear conflitos de unique para `409`
- [x] Padronizar mensagens para violacoes esperadas da feature

---

## Sprint 2 - Frontend Reutilizavel + UX

## 7) Servico Angular
- [x] Criar `frontend/src/app/features/config-agrupadores/agrupadores-empresa.service.ts`
- [x] Implementar metodos para todos os endpoints REST da feature
- [x] Tratar headers de tenant conforme padrao atual do projeto

## 8) Componente reutilizavel
- [x] Criar componente standalone:
- [x] `agrupadores-empresa.component.ts`
- [x] `agrupadores-empresa.component.html`
- [x] `agrupadores-empresa.component.css`
- [x] Inputs:
- [x] `configType: string`
- [x] `configId: number`
- [x] `readonly?: boolean`
- [x] Output:
- [x] `changed` (opcional)

## 9) UX funcional minima
- [x] Listar agrupadores da configuracao
- [x] Criar agrupador (nome)
- [x] Editar nome
- [x] Excluir agrupador
- [x] Gerenciar empresas do agrupador
- [x] Multi-selecao com busca
- [x] Exibir loading/erro/vazio
- [x] Responsividade (desktop/mobile)

## 10) UX de conflito de empresa ja vinculada
- [x] Bloquear no UI empresas ja vinculadas em outro agrupador da mesma configuracao
- [x] Mostrar em qual agrupador a empresa ja esta
- [x] Tratar fallback da API (`409/422`) com mensagem amigavel

## 11) Embutir em telas de configuracao
- [x] Integrar componente em 1 tela piloto de configuracao
- [x] Validar contrato `configType/configId`
- [x] Preparar padrao de embedding para demais telas

---

## Sprint 3 - Qualidade, Concorrencia e Hardening

## 12) Testes backend
- [x] Unitarios de servico (`AgrupadorEmpresaServiceTest`)
- [x] Integracao de repositorio e constraints
- [x] Teste concorrente com duas transacoes simultaneas para mesma empresa/configuracao
- [x] Resultado esperado: 1 sucesso + 1 conflito

## 13) Testes frontend
- [x] Unit tests do servico e componente
- [x] Cenarios de erro API (409/422)
- [x] Estados de loading e erro

## 14) E2E
- [x] Criar 2 agrupadores na mesma configuracao
- [x] Tentar vincular a mesma empresa nos 2
- [x] Validar bloqueio/erro claro
- [x] Excluir agrupador e reaproveitar empresa em outro

## 15) Observabilidade e robustez
- [x] Logs tecnicos minimos para erros de vinculo
- [x] Metricas basicas de falhas de conflito (opcional)
- [x] Revisao de permissoes por endpoint

---

## Definicao de Pronto Global (DoD)
- [x] Migration aplicada sem quebra em ambiente local
- [x] API completa da feature disponivel e validada
- [x] Componente Angular reutilizavel funcionando em ao menos 1 tela real
- [x] Regra critica garantida por banco + backend + UI
- [x] Testes automatizados principais passando
- [x] Documentacao tecnica minima atualizada (README interno da feature ou comentario em `plans/`)

---

## Rollback por etapa

## Etapa DB/API
- [ ] Desligar exposicao no frontend (feature off)
- [ ] Manter migration sem uso funcional ate correcao

## Etapa Frontend
- [ ] Remover embedding do componente em telas afetadas
- [ ] Preservar backend para retrabalho incremental

## Etapa Hardening
- [ ] Reverter apenas ajustes avancados de UX/observabilidade
- [ ] Manter nucleo transacional e constraints
