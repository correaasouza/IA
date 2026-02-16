# catalogo-crud-feature

## Contexto
- O modulo de Catalogo (Produtos e Servicos) foi implementado com CRUD e configuracao por agrupador.
- Foi acordado que a experiencia dessas telas deve seguir o mesmo padrao usado em Entidades (`EntityRecords`).

## Objetivo
- Garantir consistencia de UX entre Catalogo e Entidades.
- Reduzir curva de aprendizado e manter previsibilidade de navegacao para o usuario final.

## Escopo
- Padronizar listas de Produtos/Servicos com o layout de Entidades.
- Manter contexto de empresa e agrupador no header.
- Usar filtro principal com `field-search` e status no mesmo bloco de filtros.
- Exibir treeview de grupos no modelo inline retratil (mesmo padrao de acao de grupos).
- Atualizar documentacao do modulo reforcando explicitamente o padrao adotado.

## Passos acordados
1. Ajustar `catalog-items-list` para usar o mesmo fluxo visual de Entidades.
2. Reposicionar controles de grupos para o header de filtros.
3. Garantir comportamento responsivo igual ao padrao ja consolidado nas listas de Entidades.
4. Registrar o padrao no `frontend/src/app/features/catalog/README.md`.
5. Registrar este acordo em `plans/` para continuidade das proximas iteracoes.

## Criterios de conclusao
- Produtos e Servicos apresentam estrutura de tela equivalente a Entidades (header, filtros, grupos e lista).
- Treeview de grupos abre/fecha no bloco de filtros e filtra a lista.
- Documentacao do modulo contem secao explicita sobre o padrao de UX baseado em Entidades.
