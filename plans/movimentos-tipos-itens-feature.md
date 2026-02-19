# movimentos-tipos-itens-feature

## Contexto
O sistema ja possui configuracoes de movimentos e operacao de MOVIMENTO_ESTOQUE, com arquitetura multi-tenant e padroes de lista/ficha no frontend.
A evolucao desta entrega adiciona tipos de itens por locatario, vinculacao por configuracao de movimento e lancamento de itens no movimento de estoque.

## Objetivo
Permitir que cada tipo de movimento opere com tipos de itens configuraveis (produto/servico), com regra por vinculo (`cobrar`) e uso efetivo desses tipos no lancamento dos itens do movimento.

## Escopo
- Cadastro de tipos de itens por tenant.
- Configuracao de tipos de itens por configuracao de movimento.
- Inclusao de itens em MOVIMENTO_ESTOQUE com validacoes de configuracao e catalogo.
- Novas permissoes dedicadas para configuracao e operacao de itens.
- Ajustes de UI em /configs/movimentos e ficha de estoque.

## Passos acordados
1. Registrar este plano em `plans/`.
2. Criar migracoes V19-V22.
3. Implementar entidades/repositorios/servicos/controllers/DTOs no backend.
4. Integrar itens no handler de estoque e endpoint de busca de catalogo operacional.
5. Atualizar permissoes (enum, seed, migration) e tratamento de erros.
6. Atualizar frontend de configuracao e operacao com os novos contratos.
7. Criar/ajustar testes backend e frontend.
8. Executar validacoes locais de build/test.

## Criterios de conclusao
- Usuario cadastra tipos de item por tenant.
- Usuario configura tipos de item por configuracao de movimento com `cobrar`.
- Ficha de estoque salva/edita itens respeitando configuracao e catalogo.
- Regra `cobrar=false` zera preco no backend.
- Snapshot (codigo/nome) do item e persistido.
- Permissoes novas controlam acesso dos endpoints e da UI.
- Testes principais passam sem regressao dos padroes globais.
