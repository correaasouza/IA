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
