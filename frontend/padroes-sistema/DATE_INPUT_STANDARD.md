# Padrão Global de Campos de Data (Frontend)

Este projeto adota **obrigatoriamente** o padrão de data:

- Formato de entrada e exibição em campos: `DD/MM/AAAA`
- Exemplo válido: `14/02/2026`

## Regras obrigatórias

1. Todo campo de data em formulário deve usar máscara automática (`appDateMask`).
2. Placeholders, hints e mensagens de validação devem exibir `DD/MM/AAAA`.
3. Antes de enviar para API, converter usando `toIsoDate(...)` (resultado `YYYY-MM-DD`).
4. Ao preencher formulário com valor vindo da API (`YYYY-MM-DD`), converter com `toDisplayDate(...)`.
5. Validação de campo de data deve usar `isValidDateInput(...)`.
6. Não criar novos parse/format de data fora de `frontend/src/app/shared/date-utils.ts`.

## Preenchimento automático

Com máscara `appDateMask`:

- Digitar `DD` e sair do campo -> completa para `DD/MM/AAAA` com mês/ano atuais.
- Digitar `DDMM` e sair do campo -> completa para `DD/MM/AAAA` com ano atual.

## Referências de implementação

- `frontend/src/app/shared/date-mask.directive.ts`
- `frontend/src/app/shared/date-utils.ts`

