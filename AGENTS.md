# Repository Guidelines

## Project Structure & Module Organization

- `backend/`: API Java (Spring Boot). Código em `backend/src/main/java`, configs em `backend/src/main/resources` (ex.: `application.yml`) e migrações Flyway em `backend/src/main/resources/db/migration`.
- `backend/src/test/java`: testes (JUnit via `spring-boot-starter-test`).
- `frontend/`: app Angular. Código em `frontend/src/app`, assets em `frontend/src/assets`, ambientes em `frontend/src/environments`.
- `infra/`: recursos de infraestrutura (ex.: `infra/realm-import.json`, `infra/db-init.sql`, tema do Keycloak em `infra/keycloak-theme/...`).
- `docker-compose.yml`: sobe Postgres, Keycloak, backend e frontend para ambiente local.

## Build, Test, and Development Commands

- `docker compose up --build`: sobe o stack completo (Postgres/Keycloak/backend/frontend).
- `mvn -f backend/pom.xml test`: roda testes do backend.
- `mvn -f backend/pom.xml spring-boot:run`: executa o backend localmente (Java 21).
- `npm --prefix frontend install`: instala dependências do frontend.
- `npm --prefix frontend start`: `ng serve` para dev.
- `npm --prefix frontend test`: executa testes do Angular.
- `npm --prefix frontend run build`: gera build do frontend.

## Coding Style & Naming Conventions

- Indentação: 2 espaços no frontend (TS/HTML/CSS) e 2 ou 4 espaços no backend (Java), mantendo consistência no arquivo existente.
- Java: siga convenções Java/Spring (classes `PascalCase`, métodos/variáveis `camelCase`, pacotes em `lowercase`, ex.: `com.ia...`).
- Angular: componentes/serviços em `kebab-case` (ex.: `user-form.component.ts`) e classes `PascalCase`.

## Testing Guidelines

- Backend: adicione testes em `backend/src/test/java` com sufixo `*Test` (ex.: `CpfCnpjValidatorTest`).
- Frontend: mantenha testes junto ao código (padrão Angular) e valide com `npm --prefix frontend test`.

## Commit & Pull Request Guidelines

- Commits: prefira mensagens no estilo Conventional Commits quando aplicável (ex.: `feat(ui): ...`). Para refactors, use imperativo claro (ex.: `Refactor ...`).
- PRs devem incluir:
  - descrição objetiva do problema/solução e passos para reproduzir/validar;
  - referência a issue/ticket (quando existir);
  - screenshot/clip para mudanças visuais no `frontend/`;
  - nota de impacto em migrações/config (ex.: novos scripts em `db/migration`).

## Security & Configuration Tips

- `docker-compose.yml` contém credenciais padrão (ex.: `admin/admin123`) para desenvolvimento. Não reutilize em produção.
- Para evitar mismatch de `issuer` (claim `iss` do token) entre Keycloak e backend, prefira acessar o UI por `http://host.docker.internal:8080` quando estiver usando o stack via Docker.
