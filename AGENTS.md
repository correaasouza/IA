# Repository Guidelines

## Project Structure & Module Organization

- `backend/`: API Java (Spring Boot). Codigo em `backend/src/main/java`, configs em `backend/src/main/resources` (ex.: `application.yml`) e migracoes Flyway em `backend/src/main/resources/db/migration`.
- `backend/src/test/java`: testes (JUnit via `spring-boot-starter-test`).
- `frontend/`: app Angular. Codigo em `frontend/src/app`, assets em `frontend/src/assets`, ambientes em `frontend/src/environments`.
- `infra/`: recursos de infraestrutura (ex.: `infra/realm-import.json`, `infra/db-init.sql`, tema do Keycloak em `infra/keycloak-theme/...`).
- `docker-compose.yml`: sobe Postgres, Keycloak, backend e frontend para ambiente local.

## Build, Test, and Development Commands

- `docker compose up --build`: sobe o stack completo (Postgres/Keycloak/backend/frontend).
- `mvn -f backend/pom.xml test`: roda testes do backend.
- `mvn -f backend/pom.xml spring-boot:run`: executa o backend localmente (Java 21).
- `npm --prefix frontend install`: instala dependencias do frontend.
- `npm --prefix frontend start`: `ng serve` para dev.
- `npm --prefix frontend test`: executa testes do Angular.
- `npm --prefix frontend run build`: gera build do frontend.

## Coding Style & Naming Conventions

- Indentacao: 2 espacos no frontend (TS/HTML/CSS) e 2 ou 4 espacos no backend (Java), mantendo consistencia no arquivo existente.
- Java: siga convencoes Java/Spring (classes `PascalCase`, metodos/variaveis `camelCase`, pacotes em `lowercase`, ex.: `com.ia...`).
- Angular: componentes/servicos em `kebab-case` (ex.: `user-form.component.ts`) e classes `PascalCase`.

## Testing Guidelines

- Backend: adicione testes em `backend/src/test/java` com sufixo `*Test` (ex.: `CpfCnpjValidatorTest`).
- Frontend: mantenha testes junto ao codigo (padrao Angular) e valide com `npm --prefix frontend test`.

## Commit & Pull Request Guidelines

- Commits: prefira mensagens no estilo Conventional Commits quando aplicavel (ex.: `feat(ui): ...`). Para refactors, use imperativo claro (ex.: `Refactor ...`).
- PRs devem incluir:
  - descricao objetiva do problema/solucao e passos para reproduzir/validar;
  - referencia a issue/ticket (quando existir);
  - screenshot/clip para mudancas visuais no `frontend/`;
  - nota de impacto em migracoes/config (ex.: novos scripts em `db/migration`).

## Security & Configuration Tips

- `docker-compose.yml` contem credenciais padrao (ex.: `admin/admin123`) para desenvolvimento. Nao reutilize em producao.
- Para evitar mismatch de `issuer` (claim `iss` do token) entre Keycloak e backend, prefira acessar o UI por `http://host.docker.internal:8080` quando estiver usando o stack via Docker.

## Padroes Globais do Frontend (Obrigatorio)

### Regra base de arquitetura de telas

- Listas e fichas devem ser telas separadas.
- E proibido manter formulario inline permanente em pagina de listagem.
- Em listas, as acoes por item devem contemplar `Consultar`, `Alterar` e `Excluir` (quando aplicavel).
- Excecoes so sao aceitas com documentacao formal no proprio repositorio.

### Padrao global de listas

- Toda lista deve ter bloco superior fixo com titulo, acoes e filtros.
- O bloco superior deve usar `.page-list-sticky` e fundo opaco `var(--surface)`.
- Em layout com padding superior no container, aplicar compensacao para evitar salto visual (`margin-top` negativo e `padding-top` equivalente).
- O bloco sticky deve envolver cabecalho da lista e card de filtros.

### Coluna de acoes fixa em tabela

- Em desktop, a coluna `Acoes` deve permanecer visivel.
- Usar `stickyEnd` no `matColumnDef` da coluna `acoes`.
- O container da tabela deve usar `.table-sticky-actions`.
- Definir largura fixa adequada para os botoes (`--actions-col-width`) e manter fundo opaco com separador esquerdo.
- Na lista de usuarios, os icones da coluna `Acoes` devem ficar em linha unica (`flex-nowrap`).

### Padrao mobile para filtros de lista

- Em mobile, filtros devem iniciar ocultos.
- Deve existir botao compacto de toggle com classe `.mobile-filter-toggle`.
- Texto do botao: `Filtros` com contador quando houver filtros ativos (ex.: `Filtros (2)`).
- O botao deve exibir icone `tune` e indicador de expansao.
- Em desktop/tablet (`md+`), filtros ficam sempre visiveis.
- O comportamento mobile deve depender de regra explicita de viewport no componente.

### Padrao global de fichas

- Toda ficha deve usar header interno sticky com classe `.page-header-sticky`.
- Header deve manter fundo opaco `var(--surface)` e sem salto de altura durante scroll.
- Header deve exibir obrigatoriamente o campo principal no formato `Nome: <valor atual>` em todas as fichas de cadastro.
- `Nome: <valor atual>` deve aparecer logo abaixo do subtitulo e atualizar em tempo real durante digitacao (fallback para dado carregado da API).
- Em telas com padding superior no container, aplicar compensacao no sticky para evitar salto visual.
- Toda ficha deve ter rotas dedicadas para `novo`, `consultar` e `editar` quando aplicavel.

### Padrao de ficha em dialog/modal

- Em `mat-dialog`, usar card superior unico dentro de `mat-dialog-title` (titulo, metadados e acoes no mesmo bloco).
- Dialog de ficha segue o mesmo padrao de header das fichas de pagina, incluindo `Nome: <valor atual>` no bloco de metadados.
- Layout padrao: esquerda com titulo/metadados; direita com acoes primarias (`Cancelar/Voltar` e `Salvar`).
- Em mobile, acoes devem quebrar para duas colunas e depois uma coluna em telas estreitas.
- `mat-dialog-content` deve iniciar com espacamento que nao corte o primeiro campo.
- `mat-dialog-actions` so deve existir quando houver necessidade funcional adicional.
- Abas contextuais da ficha devem ficar na mesma linha de navegacao da ficha, sem subnivel escondido no corpo.

### Padrao global para campos de data

- Formato de entrada e exibicao: `DD/MM/AAAA` (ex.: `14/02/2026`).
- Todo campo de data de formulario deve usar mascara `appDateMask`.
- Placeholders, hints e validacoes devem seguir `DD/MM/AAAA`.
- Antes de enviar para API, converter com `toIsoDate(...)` (`YYYY-MM-DD`).
- Ao carregar valor da API (`YYYY-MM-DD`), converter com `toDisplayDate(...)`.
- Validacao deve usar `isValidDateInput(...)`.
- Nao criar parse/format de data fora de `frontend/src/app/shared/date-utils.ts`.
- Com `appDateMask`, ao sair do campo:
  - `DD` completa para `DD/MM/AAAA` com mes/ano atuais.
  - `DDMM` completa para `DD/MM/AAAA` com ano atual.

### Padrao global de controle de acesso por controle

- Diretiva oficial: `appAccessControl` (`frontend/src/app/shared/access-control.directive.ts`).
- Servico de suporte: `frontend/src/app/core/access/access-control.service.ts`.
- Todo novo botao sensivel deve usar `appAccessControl`.
- Evitar `*ngIf` hardcoded por role quando houver chave de controle.
- Contrato da diretiva:
  - `appAccessControl` (obrigatorio): chave unica (`<modulo>.<acao>`; menu com prefixo `menu.`).
  - `appAccessFallbackRoles` (opcional): fallback de papeis.
  - `appAccessHide` (opcional, default `true`): oculta sem acesso.
  - `appAccessConfigurable` (opcional, default `true`): habilita configuracao para MASTER/ADMIN.
- Regra visual:
  - Para usuarios `MASTER`/`ADMIN`, a diretiva exibe icone de escudo ao lado do controle.
  - O icone abre configuracao de papeis permitidos da chave.
- Persistencia principal por tenant no backend:
  - `GET /api/access-controls`
  - `PUT /api/access-controls/{controlKey}`
  - `DELETE /api/access-controls/{controlKey}`
- Cache local de apoio: `acl:controls:{tenantId}` no `localStorage`.
- Tela administrativa oficial: `/access-controls` (`frontend/src/app/features/access-controls/access-controls.component.ts`).
- A tela administrativa deve permitir criar chave, editar papeis permitidos e remover chave.

## Referencias de Implementacao

- Classes globais: `frontend/src/styles.css` (`.page-list-sticky`, `.table-sticky-actions`, `.page-header-sticky`).
- Listas de referencia:
  - `frontend/src/app/features/tenants/tenants-list.component.html`
  - `frontend/src/app/features/users/users-list.component.html`
  - `frontend/src/app/features/companies/companies-list.component.html`
  - `frontend/src/app/features/roles/roles.component.html`
  - `frontend/src/app/features/access-controls/access-controls.component.html`
- Fichas de referencia:
  - `frontend/src/app/features/tenants/tenant-form.component.html`
  - `frontend/src/app/features/users/user-form.component.html`
  - `frontend/src/app/features/companies/company-form.component.html`
  - `frontend/src/app/features/roles/role-form.component.html`
  - `frontend/src/app/features/access-controls/access-control-form.component.html`
  - `frontend/src/app/features/configs/agrupadores-empresa.component.html`
- Data utils e mascara:
  - `frontend/src/app/shared/date-mask.directive.ts`
  - `frontend/src/app/shared/date-utils.ts`
