# entidades-cadastro-complemento-feature

## Contexto
- O sistema ja possui cadastro de entidades via `registro_entidade` e `pessoa`, com API ativa em `/api/tipos-entidade/{tipoEntidadeId}/entidades`.
- A evolucao deve manter compatibilidade funcional atual, incluindo configuracoes existentes por tipo e agrupador.
- Foi definido que a configuracao de grupos/campos da ficha sera por `tipo_entidade_config_agrupador`.

## Objetivo
- Complementar o cadastro de Entidades com modelagem de agregado completa (Pessoa, Documentacao, Enderecos, Contatos, Comercial, RH completo, Familiares e Fiscal), mantendo API atual.
- Padronizar validacoes de dominio e UI (CPF/CNPJ, telefone, data, percentuais, moeda, coerencia de datas).
- Garantir multiempresa por `empresa_id` obrigatorio em todo o agregado.

## Escopo
1. Banco/migracao:
- Evoluir `registro_entidade` com `empresa_id`, metadados e versao de concorrencia.
- Criar tabelas satelites do agregado de entidade e catalogos de apoio.
- Criar tabelas de configuracao de grupos/campos por contexto de agrupador.
2. Backend:
- Expandir entidades JPA, DTOs, repositorios, services e controller mantendo endpoint atual.
- Incluir validacoes de consistencia e tratamento de erros padronizado.
3. Frontend:
- Expandir ficha de entidade em secoes por bloco de dominio.
- Aplicar mascaras/validadores globais ja existentes.
- Respeitar configuracao dinamica de grupos/campos (visivel/editavel/obrigatorio/default).
4. Qualidade:
- Testes unitarios e de integracao para regras de negocio, migracao e contratos.

## Passos acordados
1. Registrar plano em `plans/`.
2. Implementar fundacao de dados:
- `empresa_id` obrigatorio no agregado.
- colunas base de negocio na raiz.
- estrutura inicial de configuracao por grupo/campo.
3. Implementar backend compativel:
- estender `RegistroEntidade`/service/controller sem quebrar contrato atual.
- adicionar contratos expandidos de leitura/escrita.
4. Implementar modulo de validacao e normalizacao de documentos e contatos.
5. Implementar RH completo (contrato + informacoes RH + referencias/cadastros de apoio).
6. Ajustar frontend da ficha de entidades para novo contrato e configuracao dinamica.
7. Executar suite de testes e revisar performance com indices.

## Criterios de conclusao
- API atual de entidades continua funcional com compatibilidade.
- `empresa_id` obrigatorio e auditavel em todo o agregado.
- Cadastro completo da entidade implementado (incluindo RH completo).
- Regras de validacao e mascaras aplicadas fim a fim.
- Configuracao por grupo/campo funcionando por `tipo_entidade_config_agrupador`.
- Migracoes Flyway aplicando com sucesso em base limpa e com dados.

## Status de execucao (2026-02-28)

### Checklist de entrega
- [x] Plano registrado em `plans/`.
- [x] Migracoes Flyway do agregado (`V5` a `V8`) implementadas.
- [x] Backend com agregado expandido (raiz + sub-recursos) implementado.
- [x] Endpoints REST de sub-recursos implementados (documentacao, endereco, contato/formas, familiar, comercial, fiscal, RH, referencias, qualificacoes).
- [x] Ficha frontend expandida com secoes do agregado.
- [x] CRUD frontend para sub-recursos principais (incluindo edicao inline para endereco/contato/forma/familiar).
- [x] Busca assistida de familiar com autocomplete e selecao direta.
- [x] Build backend e frontend em estado compilavel.
- [x] Testes automatizados 100% verdes.
- [x] Validacao visual final em ambiente funcional com QA de fluxo ponta a ponta.

### Resultado de testes nesta execucao
- Backend:
  - Comando: `mvn -f backend/pom.xml test`
  - Resultado: sucesso.
- Frontend:
  - Comando: `npm --prefix frontend test -- --watch=false --browsers=ChromeHeadless`
  - Resultado: 48 sucesso / 0 falhas.

### Observacoes de encerramento
- O escopo funcional principal do complemento de cadastro de entidades foi implementado e esta compilando.
- Suite de testes backend/frontend validada com sucesso apos ajuste de regra de master global e alinhamento de expectativa de mensagem no teste de agrupadores.
- Validacao final registrada em 2026-02-28 01:30:47 (ambiente funcional ativo, backend com health `UP`).

## Padrao visual adicional (2026-02-28)
- Header de fichas no mobile: botoes de acao (`Voltar`, `Salvar`, `Excluir` e equivalentes) devem permanecer em uma unica linha.
- Implementacao global: em `frontend/src/styles.css`, dentro de `@media (max-width: 767px)`, os containers de acoes do header (`.page-header-sticky > .flex.flex-wrap.gap-2` e `[class*='header-actions']`) usam `flex-wrap: nowrap` e `overflow-x: auto`.
- Regra de botoes: os botoes do header no mobile usam `flex: 0 0 auto` com `white-space: nowrap` para evitar quebra em duas linhas.
- Ajuste local da ficha de entidades: removida a regra que forçava quebra em 2 colunas/1 coluna no mobile para os botoes do header.

- Ordem padrao dos botoes de cabecalho (fichas): manter a acao destrutiva (Excluir/Inativar) imediatamente a esquerda de Salvar em desktop e mobile.
- Sequencia recomendada do grupo de acoes: Voltar, Alterar/Editar (quando existir), Desativar/Ativar (quando existir), Excluir/Inativar, Salvar.

## Padrao de acesso unitario da ficha de entidades (2026-02-28)
- Controle por grupo: chaves entities.form.group.<groupKey>.view e entities.form.group.<groupKey>.edit.
- Controle por campo: chaves entities.form.field.<fieldKey>.view e entities.form.field.<fieldKey>.edit.
- Fallback padrao de papeis: MASTER e ADMIN.
- Regras de aplicacao: sem permissao iew o grupo/campo nao e exibido; sem permissao edit o campo permanece somente leitura e acoes de manutencao do grupo ficam bloqueadas.
- Configuracao da ficha por agrupador foi revisada para cobrir todos os campos hoje exibidos na tela de entidades (dados, observacoes, enderecos, contatos, documentacao, comercial/fiscal, RH e familiares/referencias/qualificacoes).
