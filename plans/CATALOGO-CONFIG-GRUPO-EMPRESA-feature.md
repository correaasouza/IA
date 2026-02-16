# CATALOGO-CONFIG-GRUPO-EMPRESA-feature

## Resumo da revisão
- Catálogo passa a ter **configuração por agrupador de empresas** (como em Tipo de Entidades).
- Mantém `catalog_configuration` por tenant/tipo (`PRODUCTS` e `SERVICES`) como raiz.
- Cada agrupador dentro do escopo `CATALOGO` recebe sua própria configuração de `numberingMode`.

## Backend
1. Escopo de agrupador
- `ConfiguracaoScopeService` suporta `TYPE_CATALOGO`.
- `ConfiguracaoPermissaoGuard` permite gerenciar agrupadores de catálogo com `CONFIG_EDITOR`.

2. Tabela por agrupador
- Nova migration: `V4__catalog_configuration_group.sql`.
- Nova entidade: `CatalogConfigurationByGroup`.
- Regras:
  - unicidade ativa por `(tenant_id, catalog_configuration_id, agrupador_id, active)`;
  - `numbering_mode` obrigatório (`AUTOMATICA`/`MANUAL`).

3. Serviços
- `CatalogConfigurationGroupSyncService`:
  - cria config default por agrupador ao criar agrupador;
  - inativa ao remover agrupador.
- `CatalogConfigurationByGroupService`:
  - lista configs por agrupador;
  - atualiza `numberingMode` por agrupador.

4. API
- `GET /api/catalog/configuration/{type}/group-config`
- `PUT /api/catalog/configuration/{type}/group-config/{agrupadorId}`

5. Integração com agrupadores
- `AgrupadorEmpresaService` passa a chamar sync quando `configType = CATALOGO`.

## Frontend
1. Tela de catálogo
- Continua por abas `Produtos`/`Serviços`.
- Inclui seção “Configurações por Grupo de Empresas”.

2. Reuso do componente genérico
- Reutiliza `app-agrupadores-empresa` no modo:
  - `configType = 'CATALOGO'`
  - `configId = config.id`.

3. Modal de configuração por agrupador
- Abre pelo evento `configure` do agrupador.
- Campo funcional por agrupador:
  - Numeração automática
  - Numeração manual
- Salva via `updateByGroup`.

4. Service Angular
- `CatalogConfigurationService` ganhou:
  - `listByGroup(type)`
  - `updateByGroup(type, agrupadorId, payload)`

## Testes
1. Backend
- `CatalogConfigurationByGroupServiceTest`:
  - lazy create das linhas por agrupador ao listar;
  - update de `numberingMode` por agrupador.

2. Frontend
- `catalog-configuration.service.spec.ts` cobre endpoints de grupo.
