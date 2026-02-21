# Fonte das unidades oficiais

Arquivo: `nfe-official-units.json`

Origem de referencia:
- Portal Nacional da NF-e
- Tabela de Unidade Comercial (coluna de sigla/codigo de unidade)

Observacoes:
- Este arquivo e uma copia versionada para permitir seed offline e idempotente.
- Atualizacoes devem manter `codigoOficial` unico e em caixa alta.
- O seed grava `origem = NFE_TABELA_UNIDADE_COMERCIAL` para os registros inseridos via arquivo.
