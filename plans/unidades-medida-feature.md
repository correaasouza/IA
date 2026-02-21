# unidades-medida-feature

## Contexto
O ERP ja possui Entidades, Catalogo (produtos/servicos) e Movimentos (incluindo Movimento de Estoque + itens), com arquitetura multi-tenant.
Falta padronizar unidade de medida de forma global (sistema), por locatario e operacional nos itens de catalogo/movimento com historico consistente no tempo.

## Objetivo
Implementar modulo de Unidades de Medida com:
1. Cadastro global de Unidades Oficiais.
2. Seed oficial da tabela de unidades do Portal NF-e (idempotente e offline).
3. Espelhamento obrigatorio por locatario.
4. Conversoes por locatario (com reversa automatica consistente).
5. Unidade base e alternativa no catalogo com lock apos primeira movimentacao.
6. Itens de movimento com unidade escolhida, fator aplicado e quantidade convertida para base.
7. Historico de estoque congelando unidade/fator/quantidades no momento do lancamento.

## Escopo
1. Backend completo: entidades, repositorios, migracoes, seeds, servicos, APIs, seguranca e testes.
2. Frontend completo: rotas, menu, services, telas de lista/ficha para unidades oficiais, unidades do locatario e conversoes; ajustes em catalogo e movimentos.
3. Documentacao interna curta de conversao e congelamento historico.

## Passos acordados
1. Salvar plano final em `plans/unidades-medida-feature.md`.
2. Criar migrations para:
   - `official_unit`, `tenant_unit`, `tenant_unit_conversion`;
   - campos de unidade no catalogo;
   - campos de unidade/conversao no item de movimento;
   - snapshot de unidade/fator no historico (`catalog_movement`).
3. Implementar dominio/repositories/DTOs/services/controllers do backend.
4. Implementar seed global oficial por resource JSON e reconciliacao por tenant.
5. Integrar lock de unidade do catalogo ao primeiro evento de movimentacao de estoque.
6. Integrar conversao de unidade no fluxo de criacao/edicao de itens de movimento.
7. Expor endpoints para unidades oficiais, unidades do tenant, conversoes e preview/lista de unidades permitidas para item de movimento.
8. Atualizar frontend com telas novas e campos nos modulos catalogo/movimentos.
9. Cobrir testes obrigatorios de seed, conversao, lock e historico.
10. Atualizar README interno do catalogo com regras de unidade e historico.

## Criterios de conclusao
1. Master no tenant master consegue manter Unidades Oficiais.
2. Cada tenant possui unidades espelhadas das oficiais, sem duplicacao em reexecucoes.
3. Catalogo exige unidade base, permite alternativa+fator e bloqueia alteracao apos primeira movimentacao.
4. Item de movimento aceita apenas unidades permitidas e grava fator/quantidade convertida/base.
5. Historico de estoque registra unidade informada, unidade base, quantidade convertida e fator aplicado, sem recalculo retroativo.
6. Testes automatizados obrigatorios passam para seed, conversao, lock e snapshot historico.

## Decisoes fechadas
1. IDs das novas tabelas de unidade: `UUID`.
2. Escopo no catalogo: produtos e servicos.
3. Regra financeira na troca de unidade do item: manter valor total e recalcular valor unitario.
4. Conversao reversa no tenant: auto-gerada (1/fator, escala fixa).
5. Backfill de itens legados sem unidade: unidade `UN`.
6. Implementacao sem commits nesta execucao.
