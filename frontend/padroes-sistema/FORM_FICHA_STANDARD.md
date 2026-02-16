# Padrao Global de Fichas (Frontend)

Este documento define o padrao obrigatorio para fichas de cadastro/consulta/edicao.

Regra estrutural: formulario/ficha nao deve permanecer embutido em tela de lista.

## Header da ficha

1. Todo formulario de ficha deve usar header interno com classe `page-header-sticky`.
2. O header deve permanecer visivel durante o scroll da pagina da ficha.
3. O header deve ter fundo opaco (`var(--surface)`) para nao mostrar conteudo por tras.
4. O header deve exibir o campo principal no formato:
   - `Nome: <valor atual do campo>`
5. O valor exibido deve refletir digitacao em tempo real (form control), com fallback para valor carregado da API.
6. O header sticky nao pode ter salto de altura durante o scroll.
7. Em telas com container de conteudo com padding superior, aplicar compensacao no header sticky:
   - `margin-top` negativo equivalente ao padding do container.
   - `padding-top` equivalente para manter o espacamento interno.
8. O fundo opaco deve cobrir toda a area sticky para impedir vazamento visual (incluindo pseudo-elemento de cobertura).

## Header de ficha em dialog/modal

1. Quando a ficha for renderizada em `mat-dialog`, o bloco de `mat-dialog-title` deve usar o mesmo conceito visual do header de ficha: **card superior unico** com borda, raio e fundo `var(--surface)`.
2. Estrutura obrigatoria do card:
   - esquerda: titulo da ficha + metadados (`ID`, `Origem`, `Nome: <valor atual>` quando aplicavel);
   - direita: acoes principais da ficha (`Cancelar/Voltar` e `Salvar`) com os mesmos estilos de botao usados nas fichas de pagina.
3. O alinhamento deve ser `justify-between`, sem icone de fechar solto fora do padrao do card.
4. Em mobile, as acoes devem quebrar para duas colunas e depois uma coluna em telas muito estreitas.
5. O `mat-dialog-content` deve iniciar com espacamento vertical suficiente para nao cortar `mat-form-field`/label flutuante da primeira linha.
6. Footer de acoes (`mat-dialog-actions`) so deve existir quando houver necessidade funcional adicional; para fichas padrao, as acoes ficam no header-card.
7. Quando a ficha tiver abas contextuais (ex.: `Filiais`, `Configuracoes`, `Tipos de estoque`), todas devem ficar na mesma linha de navegacao da ficha, evitando subnivel de abas escondido dentro do corpo.

## Regras de navegacao

1. Toda ficha deve ter rota dedicada para `novo`, `consultar` e `editar` quando aplicavel.
2. A lista correspondente deve navegar para a ficha por botoes de acao (`Consultar`, `Alterar`, `Novo`).
3. E proibido editar registro diretamente na linha da lista quando existir ficha dedicada.

## Implementacao de referencia

- Classe global: `frontend/src/styles.css` (`.page-header-sticky`)
- Ficha de locatario: `frontend/src/app/features/tenants/tenant-form.component.html`
- Ficha de usuario: `frontend/src/app/features/users/user-form.component.html`
- Ficha de empresa: `frontend/src/app/features/companies/company-form.component.html`
- Ficha de papel: `frontend/src/app/features/roles/role-form.component.html`
- Ficha de acessos UI: `frontend/src/app/features/access-controls/access-control-form.component.html`
- Ficha em dialog (agrupadores): `frontend/src/app/features/configs/agrupadores-empresa.component.html`

## Observacao

- Para padrao de data, seguir `frontend/padroes-sistema/DATE_INPUT_STANDARD.md`.
