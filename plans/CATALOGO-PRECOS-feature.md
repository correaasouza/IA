# CATALOGO-PRECOS-feature

## Contexto
ERP multi-tenant com catalogo por agrupador de empresas. O catalogo precisa operar com 4 precos finais por item (Compra, Custo, Custo Medio e Venda Base), regras configuraveis por agrupador e tabela de precos de venda por book/variant com fallback.

## Objetivo
Implementar modulo de precos do catalogo com:
- modelagem e migrations;
- CRUD REST de PriceBook, PriceVariant e SalePrice (grid + upsert em lote);
- endpoint de resolucao de preco com fallback e source detalhado;
- configuracao por agrupador para regras de preco (base, ajuste default e lock mode);
- historico de alteracoes em sale_price;
- telas Angular simples para books, variants e grid por book+variant.

## Escopo
- Cobertura para PRODUCTS e SERVICES.
- PriceBook vinculado a registro_entidade (price_book_id nullable).
- Variant base representada por variant_id null.
- Precisao monetaria/calc: NUMERIC(19,6), HALF_UP.
- Sem integracao de snapshot em movimento nesta entrega (fase posterior).

## Passos acordados
1. Criar migrations para:
- catalog_price_rule_by_group;
- catalog_item_price;
- price_book;
- price_variant;
- sale_price;
- price_change_log;
- coluna registro_entidade.price_book_id.
2. Criar enums, entidades JPA e repositories do modulo de precos.
3. Implementar servicos:
- PriceBookService (default lazy por tenant);
- PriceVariantService;
- SalePriceService (bulk upsert/delete + logs);
- SalePriceResolverService (fallback + source);
- CatalogPriceRuleService e CatalogItemPriceService (lock modes I-IV, sync modo III, validacao de ciclo/base).
4. Expor APIs REST e integrar com CatalogConfigurationController e CatalogItemController.
5. Adicionar permissao de modulo de precos e seed padrao.
6. Implementar frontend Angular:
- telas de PriceBooks e PriceVariants;
- grid de SalePrice por book+variant com edicao inline e salvar lote;
- aba de precos no fluxo de configuracao de catalogo por agrupador;
- ajustes no formulario de item para os 4 precos.
7. Cobrir testes backend/frontend para fallback, lock/sync, historico e validacoes de configuracao.

## Criterios de conclusao
- Migrations aplicam sem erro e schema atende regras de negocio.
- APIs REST respondem conforme contratos com validacoes e fallback corretos.
- Historico price_change_log registra CREATE/UPDATE/DELETE.
- UI permite manutencao operacional de books, variants e tabela de precos.
- Testes automatizados essenciais do modulo passam.
