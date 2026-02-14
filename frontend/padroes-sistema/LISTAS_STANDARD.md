# Padrao Global de Listas (Frontend)

Este documento define o padrao obrigatorio para telas de listagem.

## Separacao lista x formulario

1. Tela de lista nao deve exibir formulario de cadastro/edicao no mesmo layout.
2. Na lista, as acoes padrao devem estar disponiveis por item: `Consultar`, `Alterar` e `Excluir`.
3. Formularios devem abrir em tela/ficha dedicada (ou drawer/dialog acionado por uma das acoes), nunca embutidos permanentemente na tela de lista.
4. E proibido manter bloco de `Novo` com campos de formulario dentro da tela de listagem.
5. E proibido manter campos editaveis inline dentro de linha da tabela/card da lista.
6. Excecoes so sao aceitas com documentacao formal de padrao especifico no diretorio `frontend/padroes-sistema/`.

## Bloco superior sempre visivel

1. Toda lista deve ter um bloco superior fixo com titulo, acoes e filtros.
2. O bloco superior deve usar a classe global `page-list-sticky`.
3. O bloco deve ficar no topo da area de conteudo (`top: 0`) e permanecer visivel durante o scroll.
4. O bloco deve usar fundo opaco (`var(--surface)`), sem transparencia, para impedir que conteudo de tras apareca.
5. O bloco sticky nao deve criar faixa superior extra ao fixar (sem salto visual).
6. O fundo opaco deve cobrir toda a area sticky para bloquear vazamento visual durante o scroll.
7. Em telas com container de conteudo com padding superior, compensar no sticky para evitar salto visual:
   - `margin-top` negativo equivalente ao padding do container.
   - `padding-top` equivalente para manter o espacamento interno.
8. O bloco fixo deve envolver:
   - cabecalho da lista (titulo/subtitulo e botoes)
   - card de filtros

## Coluna de acoes sempre visivel

1. A coluna `Acoes` deve permanecer sempre visivel no desktop, mesmo com muitas colunas e scroll horizontal.
2. A coluna `Acoes` deve ficar fixada na direita da tabela usando `stickyEnd` no `matColumnDef`.
3. O container da tabela deve usar a classe `table-sticky-actions`.
4. A coluna `Acoes` deve ter largura fixa padrao (`--actions-col-width`) suficiente para todos os icones da tela, sem ocultar botoes.
5. Header e celulas da coluna fixa devem ter fundo opaco (`var(--surface)`) e separador esquerdo.
6. O label `Acoes` deve alinhar visualmente com o grupo de icones da coluna.

## Regra mobile para filtros

1. Em telas mobile, os filtros devem iniciar ocultos para priorizar area util da lista.
2. Deve existir botao pequeno no estilo compacto (icone + texto), seguindo visual discreto e alinhado a direita.
3. O texto do botao deve seguir o padrao `Filtros` e exibir contador quando houver filtros ativos, por exemplo: `Filtros (2)`.
4. Em desktop/tablet (`md+`), os filtros permanecem sempre visiveis.
5. O botao de toggle e comportamento colapsavel devem ser padrao em todas as listas do sistema.
6. A exibicao/ocultacao no mobile deve usar regra explicita de viewport (sem depender apenas de classes utilitarias).
7. O botao deve usar a classe global `mobile-filter-toggle`.
8. O botao deve exibir icone de filtro (`tune`) e indicador de expansao no final.

## Classe global

- Arquivo: `frontend/src/styles.css`
- Classe: `.page-list-sticky`
- Classe: `.table-sticky-actions`

## Implementacoes de referencia

- `frontend/src/app/features/tenants/tenants-list.component.html`
- `frontend/src/app/features/users/users-list.component.html`
- `frontend/src/app/features/companies/companies-list.component.html`
- `frontend/src/app/features/roles/roles.component.html`
- `frontend/src/app/features/access-controls/access-controls.component.html`

## Regra obrigatoria para usuarios

1. A lista de usuarios deve seguir o mesmo padrao de coluna `Acoes` fixa da lista de locatarios e papeis.
2. Em `md+`, usar `table-sticky-actions` no container da tabela e `stickyEnd` na coluna `acoes`.
3. Os icones em `Acoes` na lista de usuarios devem ficar em linha unica (`flex-nowrap`).
