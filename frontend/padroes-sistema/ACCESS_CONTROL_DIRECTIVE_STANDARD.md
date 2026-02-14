# Padrao Global de Controle de Acesso por Controle (Diretiva)

Este documento define o padrao oficial para controle de acesso por botao/acao/componente no frontend.

## Objetivo

1. Eliminar dependencia de papeis fixos hardcoded em cada tela.
2. Permitir configuracao de acesso diretamente no controle (botao/acao) via diretiva reutilizavel.
3. Padronizar a evolucao de autorizacao para todos os componentes futuros.

## Diretiva oficial

- Nome: `appAccessControl`
- Arquivo: `frontend/src/app/shared/access-control.directive.ts`
- Servico de suporte: `frontend/src/app/core/access/access-control.service.ts`

## Como usar

Exemplo basico:

```html
<button
  mat-flat-button
  appAccessControl="users.create"
  [appAccessFallbackRoles]="['MASTER_ADMIN']">
  Novo usuário
</button>
```

## Contrato da diretiva

1. `appAccessControl` (obrigatorio): chave unica do controle.  
Exemplos: `users.create`, `users.delete`, `menu.users`.
2. `appAccessFallbackRoles` (opcional): papeis padrao caso nao exista configuracao customizada.
3. `appAccessHide` (opcional, default `true`): quando sem acesso, oculta o controle.
4. `appAccessConfigurable` (opcional, default `true`): habilita icone de configuracao para MASTER/ADMIN.

## Regra de configuracao visual

1. Para usuários com papel `MASTER` ou `ADMIN`, a diretiva exibe um ícone pequeno de escudo ao lado do controle.
2. Esse icone abre configuracao de papeis permitidos para a chave do controle.
3. A configuracao e persistida por tenant no `localStorage`.

## Persistencia

1. Persistencia principal no backend por tenant via API:
   - `GET /api/access-controls`
   - `PUT /api/access-controls/{controlKey}`
   - `DELETE /api/access-controls/{controlKey}`
2. Cache local de apoio no frontend:
   - `acl:controls:{tenantId}` no `localStorage`.
3. Se nao houver configuracao para a chave, aplica `appAccessFallbackRoles`.

## Convencao de chaves

1. Use padrao `<modulo>.<acao>`.
2. Prefixo `menu.` para itens de menu.
3. Chaves devem ser estaveis e sem espaco.

## Regras de adocao

1. Todo novo botao sensivel deve usar `appAccessControl`.
2. Evitar `*ngIf` hardcoded por role em tela quando houver chave de controle.
3. Regras antigas em `roles/perms` podem permanecer apenas como fallback durante migracao.

## Tela administrativa

1. Rota oficial: `/access-controls`.
2. Componente: `frontend/src/app/features/access-controls/access-controls.component.ts`.
3. A tela permite:
   - criar chave de controle;
   - editar papeis permitidos;
   - remover chave.
4. Acesso da tela: `MASTER` ou `ADMIN`.

