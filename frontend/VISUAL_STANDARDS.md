# Padroes Visuais do Frontend (Referencia para IA)

Este documento define os padroes obrigatorios para novas telas e ajustes visuais no frontend.

## Controles de entrada (padrao global)

- Altura padrao: `36px`.
- Aplica para:
  - `mat-form-field` (`input`, `select`, `textarea` quando usado como campo de entrada).
  - `select` nativo no estilo global (`.status-search-like` + `.status-native-select`).
  - `app-field-search` (via `--filter-h: 36px`).
- Nao criar variacoes locais de altura como `24px`, `30px`, `42px` ou `52px` em fichas.

## Fichas (cadastro/consulta/edicao)

- Toda ficha deve usar `.page-form-shell`.
- Fichas em dialog/modal devem usar `.erp-form-dialog-dense` e manter os mesmos `36px`.
- Priorizar consistencia visual entre todos os controles do formulario.
- Grid responsivo padrao de ficha:
  - `>=1600px`: 6 colunas
  - `>=1200px`: 4 colunas
  - `>=900px`: 3 colunas
  - `>=640px`: 2 colunas
  - `<640px`: 1 coluna

## Filtros de listas

- Filtros em `form[role='search']` seguem o mesmo padrao visual de `36px`.
- Em listas com `select` nativo de filtro, usar sempre `.status-search-like` + `.status-native-select`.

## Regra para novas telas geradas por IA

- Antes de criar CSS local para altura de campo, reutilizar o padrao global em `frontend/src/styles.css`.
- Se um caso exigir excecao, documentar no proprio PR e no arquivo de referencia da feature.
