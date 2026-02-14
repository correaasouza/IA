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

## Implementacao de referencia

- Classe global: `frontend/src/styles.css` (`.page-header-sticky`)
- Ficha de locatario: `frontend/src/app/features/tenants/tenant-form.component.html`
- Ficha de usuario: `frontend/src/app/features/users/user-form.component.html`
- Ficha de empresa: `frontend/src/app/features/companies/company-form.component.html`
- Ficha de papel: `frontend/src/app/features/roles/role-form.component.html`

## Observacao

- Para padrao de data, seguir `frontend/padroes-sistema/DATE_INPUT_STANDARD.md`.
