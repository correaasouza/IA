package com.ia.app.web;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(EntityNotFoundException.class)
  public ProblemDetail handleNotFound(EntityNotFoundException ex) {
    String message = ex.getMessage() == null ? "" : ex.getMessage();
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    pd.setTitle("Recurso nao encontrado");
    if (message.startsWith("catalog_item_not_found")) {
      pd.setDetail("Item de catalogo nao encontrado no contexto atual (empresa/grupo).");
    } else if (message.startsWith("catalog_group_not_found")) {
      pd.setDetail("Grupo de catalogo nao encontrado no contexto atual.");
    } else if (message.startsWith("official_unit_not_found")) {
      pd.setDetail("Unidade oficial nao encontrada.");
    } else if (message.startsWith("tenant_unit_not_found")) {
      pd.setDetail("Unidade do locatario nao encontrada.");
    } else if (message.startsWith("tenant_unit_conversion_not_found")) {
      pd.setDetail("Conversao de unidade nao encontrada.");
    } else if (message.startsWith("registro_entidade_not_found")) {
      pd.setDetail("Entidade nao encontrada no contexto atual (empresa/grupo).");
    } else if (message.startsWith("movimento_config_not_found")) {
      pd.setDetail("Configuracao de movimento nao encontrada no locatario atual.");
    } else if (message.startsWith("movimento_estoque_not_found")) {
      pd.setDetail("Movimento de estoque nao encontrado no contexto atual.");
    } else if (message.startsWith("movimento_estoque_item_not_found")) {
      pd.setDetail("Item do movimento de estoque nao encontrado no contexto atual.");
    } else if (message.startsWith("price_book_not_found")) {
      pd.setDetail("Tabela de preco nao encontrada.");
    } else if (message.startsWith("price_variant_not_found")) {
      pd.setDetail("Variacao de preco nao encontrada.");
    } else if (message.startsWith("sale_price_not_found")) {
      pd.setDetail("Preco de venda nao encontrado.");
    } else if (message.startsWith("catalog_configuration_group_not_found")) {
      pd.setDetail("Configuracao por agrupador nao encontrada para o catalogo.");
    } else if (message.startsWith("workflow_definition_not_found")) {
      pd.setDetail("Definicao de workflow nao encontrada.");
    } else if (message.startsWith("workflow_instance_not_found")) {
      pd.setDetail("Instancia de workflow nao encontrada para o registro informado.");
    } else {
      pd.setDetail(ex.getMessage());
    }
    return pd;
  }

  @ExceptionHandler(IllegalStateException.class)
  public ProblemDetail handleIllegalState(IllegalStateException ex) {
    String message = ex.getMessage() == null ? "" : ex.getMessage();
    HttpStatus status = message.startsWith("role_required_")
      ? HttpStatus.FORBIDDEN
      : message.equals("unauthorized")
        ? HttpStatus.UNAUTHORIZED
        : message.equals("movimento_config_feature_disabled")
          || message.equals("workflow_feature_disabled")
          ? HttpStatus.SERVICE_UNAVAILABLE
        : HttpStatus.BAD_REQUEST;
    ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setTitle("Requisicao invalida");
    if (message.startsWith("movimento_config_feature_disabled")) {
      pd.setDetail("Modulo de Configuracoes de Movimentos desabilitado por feature flag.");
    } else if (message.startsWith("workflow_feature_disabled")) {
      pd.setDetail("Modulo de Workflow desabilitado por feature flag.");
    } else {
      pd.setDetail(ex.getMessage());
    }
    return pd;
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
    String message = ex.getMessage() == null ? "" : ex.getMessage();
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
    pd.setTitle("Acesso negado");
    if (message.startsWith("workflow_action_undo_stock_permission_denied")) {
      pd.setDetail("Sem permissao para desfazer movimentacao de estoque nesta transicao.");
    } else if (message.startsWith("workflow_transition_access_denied")) {
      pd.setDetail("Sem permissao para executar esta transicao de workflow.");
    } else {
      pd.setDetail("Usuario sem permissao para executar esta operacao.");
    }
    return pd;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    String message = ex.getMessage() == null ? "" : ex.getMessage();
    HttpStatus status = isConflict(message) ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
    ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setTitle("Requisicao invalida");
    if (message.startsWith("usuario_email_duplicado")) {
      pd.setDetail("E-mail ja cadastrado para outro usuario.");
    } else if (message.startsWith("usuario_username_duplicado")) {
      pd.setDetail("Username ja cadastrado para outro usuario.");
    } else if (message.startsWith("agrupador_nome_duplicado")) {
      pd.setDetail("Ja existe agrupador com este nome para esta configuracao.");
    } else if (message.startsWith("empresa_ja_vinculada_outro_agrupador")) {
      pd.setDetail("Esta empresa ja esta vinculada a outro agrupador nesta configuracao.");
    } else if (message.startsWith("tipo_entidade_nome_duplicado")) {
      pd.setDetail("Ja existe tipo de entidade ativo com este nome.");
    } else if (message.startsWith("tipo_entidade_padrao_nao_excluivel")) {
      pd.setDetail("Tipos de entidade padrao nao podem ser excluidos.");
    } else if (message.startsWith("tipo_entidade_padrao_inativacao_nao_permitida")) {
      pd.setDetail("Tipos de entidade padrao nao podem ser inativados.");
    } else if (message.startsWith("tipo_entidade_config_duplicada_agrupador")) {
      pd.setDetail("Ja existe configuracao ativa para este agrupador neste tipo de entidade.");
    } else if (message.startsWith("empresa_context_required")) {
      pd.setDetail("Selecione uma empresa no sistema para continuar.");
    } else if (message.startsWith("empresa_sem_grupo_no_tipo_entidade")) {
      pd.setDetail("A empresa selecionada nao esta vinculada a nenhum Grupo de Empresas para este Tipo de Entidade.");
    } else if (message.startsWith("entidade_codigo_duplicado_configuracao")) {
      pd.setDetail("Ja existe entidade com este codigo na configuracao atual.");
    } else if (message.startsWith("pessoa_registro_federal_duplicado")) {
      pd.setDetail("Ja existe pessoa com este documento neste locatario.");
    } else if (message.startsWith("grupo_entidade_nome_duplicado_mesmo_pai")) {
      pd.setDetail("Ja existe grupo com este nome no mesmo nivel.");
    } else if (message.startsWith("grupo_entidade_possui_entidades")) {
      pd.setDetail("Nao e possivel excluir o grupo pois existem entidades vinculadas.");
    } else if (message.startsWith("grupo_entidade_ciclo_invalido")) {
      pd.setDetail("Nao e possivel mover o grupo para dentro da propria subarvore.");
    } else if (message.startsWith("catalog_configuration_type_invalid")) {
      pd.setDetail("Tipo de configuracao de catalogo invalido. Use PRODUCTS ou SERVICES.");
    } else if (message.startsWith("catalog_configuration_numbering_required")) {
      pd.setDetail("O campo numeracao e obrigatorio.");
    } else if (message.startsWith("catalog_configuration_group_duplicated")) {
      pd.setDetail("Ja existe configuracao ativa para este agrupador no catalogo.");
    } else if (message.startsWith("catalog_context_required")) {
      pd.setDetail("Selecione uma empresa no sistema para continuar.");
    } else if (message.startsWith("catalog_context_sem_grupo")) {
      pd.setDetail("A empresa selecionada nao esta vinculada a nenhum Grupo de Empresas para este catalogo.");
    } else if (message.startsWith("catalog_item_codigo_duplicado")) {
      pd.setDetail("Ja existe item com este codigo no escopo atual.");
    } else if (message.startsWith("catalog_item_codigo_required_manual")) {
      pd.setDetail("Informe o codigo manual para o item.");
    } else if (message.startsWith("catalog_item_nome_required")) {
      pd.setDetail("Informe o nome do item.");
    } else if (message.startsWith("catalog_item_nome_too_long")) {
      pd.setDetail("Nome do item excede o tamanho maximo permitido.");
    } else if (message.startsWith("catalog_item_descricao_too_long")) {
      pd.setDetail("Descricao do item excede o tamanho maximo permitido.");
    } else if (message.startsWith("catalog_item_unit_required")) {
      pd.setDetail("Informe a unidade padrao do item de catalogo.");
    } else if (message.startsWith("catalog_item_alt_unit_required")) {
      pd.setDetail("Informe a unidade alternativa ao informar fator de conversao.");
    } else if (message.startsWith("catalog_item_alt_unit_must_differ")) {
      pd.setDetail("A unidade alternativa deve ser diferente da unidade padrao.");
    } else if (message.startsWith("catalog_item_alt_unit_invalid")) {
      pd.setDetail("Unidade alternativa invalida para o locatario.");
    } else if (message.startsWith("catalog_item_alt_factor_invalid")) {
      pd.setDetail("Fator de conversao da unidade alternativa deve ser maior que zero.");
    } else if (message.startsWith("catalog_item_price_negative")) {
      pd.setDetail("Preco final do item nao pode ser negativo.");
    } else if (message.startsWith("catalog_item_price_duplicated_type_input")) {
      pd.setDetail("Entrada de preco duplicada para o mesmo tipo.");
    } else if (message.startsWith("catalog_price_rule_none_requires_mode_ii")) {
      pd.setDetail("Quando a base for NONE, o modo permitido e somente II.");
    } else if (message.startsWith("catalog_price_rule_base_required")) {
      pd.setDetail("Informe o tipo base quando a regra usar BASE_PRICE.");
    } else if (message.startsWith("catalog_price_rule_self_reference")) {
      pd.setDetail("Um tipo de preco nao pode usar ele mesmo como base.");
    } else if (message.startsWith("catalog_price_rule_none_root_required")) {
      pd.setDetail("Ao menos um tipo de preco deve ter base NONE.");
    } else if (message.startsWith("catalog_price_rule_cycle_detected")) {
      pd.setDetail("Configuracao de preco invalida: dependencia ciclica entre tipos.");
    } else if (message.startsWith("catalog_price_rule_custom_name_too_long")) {
      pd.setDetail("Nome customizado da regra excede o limite permitido.");
    } else if (message.startsWith("price_book_name_required")) {
      pd.setDetail("Informe o nome da tabela de preco.");
    } else if (message.startsWith("price_book_name_too_long")) {
      pd.setDetail("Nome da tabela de preco excede o limite permitido.");
    } else if (message.startsWith("price_book_name_duplicated")) {
      pd.setDetail("Ja existe tabela de preco com este nome.");
    } else if (message.startsWith("price_variant_name_required")) {
      pd.setDetail("Informe o nome da variacao de preco.");
    } else if (message.startsWith("price_variant_name_too_long")) {
      pd.setDetail("Nome da variacao de preco excede o limite permitido.");
    } else if (message.startsWith("price_variant_name_duplicated")) {
      pd.setDetail("Ja existe variacao de preco com este nome.");
    } else if (message.startsWith("sale_price_book_required")) {
      pd.setDetail("Informe a tabela de preco.");
    } else if (message.startsWith("sale_price_book_inactive")) {
      pd.setDetail("Tabela de preco inativa.");
    } else if (message.startsWith("sale_price_variant_inactive")) {
      pd.setDetail("Variacao de preco inativa.");
    } else if (message.startsWith("sale_price_catalog_type_required")) {
      pd.setDetail("Informe o tipo de catalogo no preco de venda.");
    } else if (message.startsWith("sale_price_catalog_item_required")) {
      pd.setDetail("Informe o item de catalogo no preco de venda.");
    } else if (message.startsWith("sale_price_scope_duplicated")) {
      pd.setDetail("Ja existe preco para a combinacao informada.");
    } else if (message.startsWith("sale_price_negative")) {
      pd.setDetail("Preco de venda nao pode ser negativo.");
    } else if (message.startsWith("catalog_item_unit_locked_by_stock_movements")) {
      pd.setDetail("Unidades do item bloqueadas por movimentacoes de estoque.");
    } else if (message.startsWith("official_unit_codigo_required")) {
      pd.setDetail("Informe o codigo oficial da unidade.");
    } else if (message.startsWith("official_unit_descricao_required")) {
      pd.setDetail("Informe a descricao da unidade oficial.");
    } else if (message.startsWith("official_unit_codigo_duplicado")) {
      pd.setDetail("Ja existe unidade oficial com este codigo.");
    } else if (message.startsWith("official_unit_codigo_immutable")) {
      pd.setDetail("Nao e permitido alterar o codigo de uma unidade oficial existente.");
    } else if (message.startsWith("official_unit_in_use")) {
      pd.setDetail("Nao e possivel excluir unidade oficial em uso por unidades de locatario.");
    } else if (message.startsWith("tenant_unit_sigla_required")) {
      pd.setDetail("Informe a sigla da unidade do locatario.");
    } else if (message.startsWith("tenant_unit_nome_required")) {
      pd.setDetail("Informe o nome da unidade do locatario.");
    } else if (message.startsWith("tenant_unit_factor_invalid")) {
      pd.setDetail("Fator para unidade oficial invalido. Use valor maior ou igual a zero.");
    } else if (message.startsWith("tenant_unit_sigla_duplicada")) {
      pd.setDetail("Ja existe unidade com esta sigla no locatario.");
    } else if (message.startsWith("tenant_unit_mirror_delete_not_allowed")) {
      pd.setDetail("Unidades espelho oficiais nao podem ser excluidas.");
    } else if (message.startsWith("tenant_unit_in_use")) {
      pd.setDetail("Nao e possivel excluir unidade em uso por catalogo ou movimentos.");
    } else if (message.startsWith("tenant_unit_conversion_origem_invalid")) {
      pd.setDetail("Unidade de origem invalida para o locatario.");
    } else if (message.startsWith("tenant_unit_conversion_destino_invalid")) {
      pd.setDetail("Unidade de destino invalida para o locatario.");
    } else if (message.startsWith("tenant_unit_conversion_pair_invalid")) {
      pd.setDetail("Origem e destino da conversao devem ser diferentes.");
    } else if (message.startsWith("tenant_unit_conversion_factor_invalid")) {
      pd.setDetail("Fator da conversao deve ser maior que zero.");
    } else if (message.startsWith("tenant_unit_conversion_duplicada")) {
      pd.setDetail("Ja existe conversao cadastrada para origem e destino informados.");
    } else if (message.startsWith("tenant_unit_conversion_not_supported")) {
      pd.setDetail("Conversao de unidade nao suportada para este item.");
    } else if (message.startsWith("catalog_group_nome_duplicado_mesmo_pai")) {
      pd.setDetail("Ja existe grupo com este nome no mesmo nivel.");
    } else if (message.startsWith("catalog_group_possui_itens")) {
      pd.setDetail("Nao e possivel excluir o grupo pois existem itens vinculados.");
    } else if (message.startsWith("catalog_group_ciclo_invalido")) {
      pd.setDetail("Nao e possivel mover o grupo para dentro da propria subarvore.");
    } else if (message.startsWith("catalog_group_nome_required")) {
      pd.setDetail("Informe o nome do grupo.");
    } else if (message.startsWith("catalog_group_nome_too_long")) {
      pd.setDetail("Nome do grupo excede o tamanho maximo permitido.");
    } else if (message.startsWith("catalog_group_id_invalid")) {
      pd.setDetail("Identificador do grupo de catalogo invalido.");
    } else if (message.startsWith("catalog_stock_metric_invalid")) {
      pd.setDetail("Metrica de movimentacao invalida. Use QUANTIDADE ou PRECO.");
    } else if (message.startsWith("catalog_stock_origin_invalid")) {
      pd.setDetail("Origem de movimentacao invalida.");
    } else if (message.startsWith("catalog_stock_timezone_invalid")) {
      pd.setDetail("Timezone de filtro invalido.");
    } else if (message.startsWith("catalog_stock_type_not_found")) {
      pd.setDetail("Tipo de estoque nao encontrado para o agrupador informado.");
    } else if (message.startsWith("catalog_stock_filial_not_found")) {
      pd.setDetail("Filial nao encontrada no locatario atual.");
    } else if (message.startsWith("catalog_stock_impact_required")) {
      pd.setDetail("Informe ao menos um impacto para registrar movimentacao de estoque.");
    } else if (message.startsWith("catalog_stock_delta_required")) {
      pd.setDetail("Delta do impacto de estoque e obrigatorio.");
    } else if (message.startsWith("catalog_stock_idempotency_required")) {
      pd.setDetail("A chave de idempotencia da movimentacao e obrigatoria.");
    } else if (message.startsWith("catalog_stock_type_codigo_required")) {
      pd.setDetail("Informe o codigo do tipo de estoque.");
    } else if (message.startsWith("catalog_stock_type_nome_required")) {
      pd.setDetail("Informe o nome do tipo de estoque.");
    } else if (message.startsWith("catalog_stock_type_codigo_duplicado")) {
      pd.setDetail("Ja existe tipo de estoque com este codigo no agrupador.");
    } else if (message.startsWith("catalog_stock_type_last_active")) {
      pd.setDetail("Mantenha ao menos um tipo de estoque ativo no agrupador.");
    } else if (message.startsWith("catalog_stock_type_id_invalid")) {
      pd.setDetail("Identificador do tipo de estoque invalido.");
    } else if (message.startsWith("catalog_stock_adjustment_codigo_required")) {
      pd.setDetail("Informe o codigo do ajuste de estoque.");
    } else if (message.startsWith("catalog_stock_adjustment_nome_required")) {
      pd.setDetail("Informe o nome do ajuste de estoque.");
    } else if (message.startsWith("catalog_stock_adjustment_tipo_required")) {
      pd.setDetail("Informe o tipo do ajuste de estoque.");
    } else if (message.startsWith("catalog_stock_adjustment_tipo_invalid")) {
      pd.setDetail("Tipo de ajuste invalido. Use ENTRADA, SAIDA ou TRANSFERENCIA.");
    } else if (message.startsWith("catalog_stock_adjustment_origem_required")) {
      pd.setDetail("Origem obrigatoria para ajuste do tipo SAIDA/TRANSFERENCIA.");
    } else if (message.startsWith("catalog_stock_adjustment_destino_required")) {
      pd.setDetail("Destino obrigatorio para ajuste do tipo ENTRADA/TRANSFERENCIA.");
    } else if (message.startsWith("catalog_stock_adjustment_origem_not_allowed")) {
      pd.setDetail("Origem nao deve ser informada para ajuste do tipo ENTRADA.");
    } else if (message.startsWith("catalog_stock_adjustment_destino_not_allowed")) {
      pd.setDetail("Destino nao deve ser informado para ajuste do tipo SAIDA.");
    } else if (message.startsWith("catalog_stock_adjustment_transferencia_requer_origem_destino")) {
      pd.setDetail("Transferencia requer origem e destino.");
    } else if (message.startsWith("catalog_stock_adjustment_same_origin_destination")) {
      pd.setDetail("Origem e destino nao podem ser iguais.");
    } else if (message.startsWith("catalog_stock_adjustment_origem_incompleto")) {
      pd.setDetail("Origem do ajuste esta incompleta.");
    } else if (message.startsWith("catalog_stock_adjustment_destino_incompleto")) {
      pd.setDetail("Destino do ajuste esta incompleto.");
    } else if (message.startsWith("catalog_stock_adjustment_agrupador_not_found")) {
      pd.setDetail("Agrupador informado no ajuste nao encontrado.");
    } else if (message.startsWith("catalog_stock_adjustment_stock_type_not_found")) {
      pd.setDetail("Tipo de estoque informado no ajuste nao encontrado.");
    } else if (message.startsWith("catalog_stock_adjustment_filial_not_found")) {
      pd.setDetail("Filial informada no ajuste nao pertence ao agrupador selecionado.");
    } else if (message.startsWith("catalog_stock_adjustment_codigo_duplicado")) {
      pd.setDetail("Ja existe ajuste de estoque com este codigo na configuracao.");
    } else if (message.startsWith("catalog_stock_adjustment_id_invalid")) {
      pd.setDetail("Identificador do ajuste de estoque invalido.");
    } else if (message.startsWith("catalog_stock_adjustment_codigo_auto_fail")) {
      pd.setDetail("Nao foi possivel gerar codigo automatico para o ajuste de estoque.");
    } else if (message.startsWith("price_change_source_invalid")) {
      pd.setDetail("Origem do historico de preco invalida.");
    } else if (message.startsWith("price_change_origin_invalid")) {
      pd.setDetail("Tipo de origem do historico de preco invalido.");
    } else if (message.startsWith("movimento_empresa_id_required")) {
      pd.setDetail("Informe a empresa do movimento.");
    } else if (message.startsWith("movimento_empresa_context_required")) {
      pd.setDetail("Selecione uma empresa no topo do sistema para operar movimentos.");
    } else if (message.startsWith("movimento_empresa_context_mismatch")) {
      pd.setDetail("A empresa do movimento deve ser igual a empresa selecionada no contexto.");
    } else if (message.startsWith("movimento_estoque_id_invalid")) {
      pd.setDetail("Identificador do movimento de estoque invalido.");
    } else if (message.startsWith("movimento_estoque_item_id_invalid")) {
      pd.setDetail("Identificador do item do movimento invalido.");
    } else if (message.startsWith("movimento_estoque_items_required")) {
      pd.setDetail("Informe ao menos um item para adicionar ao movimento.");
    } else if (message.startsWith("movimento_estoque_finalizado")) {
      pd.setDetail("Movimento finalizado. Nao e permitido alterar ou excluir.");
    } else if (message.startsWith("movimento_estoque_item_finalizado")) {
      pd.setDetail("Item finalizado. Nao e permitido alterar ou excluir.");
    } else if (message.startsWith("movimento_estoque_item_locked_by_stock_movement")) {
      pd.setDetail("Item com movimentacao de estoque. Nao e permitido alterar ou excluir.");
    } else if (message.startsWith("movimento_estoque_item_stock_movement_not_found")) {
      pd.setDetail("Nao ha movimentacao de estoque ativa para desfazer neste item.");
    } else if (message.startsWith("movimento_tipo_nao_implementado")) {
      pd.setDetail("Tipo de movimento ainda nao implementado para operacao.");
    } else if (message.startsWith("movimento_tipo_invalid")) {
      pd.setDetail("Tipo de movimento invalido.");
    } else if (message.startsWith("movimento_payload_required")) {
      pd.setDetail("Payload de movimento invalido ou ausente.");
    } else if (message.startsWith("movimento_estoque_nome_required")) {
      pd.setDetail("Informe o nome do movimento de estoque.");
    } else if (message.startsWith("movimento_estoque_version_required")) {
      pd.setDetail("Informe a versao atual do movimento para salvar alteracoes.");
    } else if (message.startsWith("movimento_estoque_item_required")) {
      pd.setDetail("Informe os dados do item do movimento.");
    } else if (message.startsWith("movimento_estoque_item_tipo_required")) {
      pd.setDetail("Informe o tipo de item do movimento.");
    } else if (message.startsWith("movimento_estoque_item_catalog_item_required")) {
      pd.setDetail("Informe o item de catalogo do movimento.");
    } else if (message.startsWith("movimento_estoque_tipo_entidade_required")) {
      pd.setDetail("Informe o tipo de entidade do movimento.");
    } else if (message.startsWith("movimento_estoque_tipo_entidade_invalid")) {
      pd.setDetail("Tipo de entidade invalido para a configuracao de movimento.");
    } else if (message.startsWith("movimento_estoque_item_quantidade_invalid")) {
      pd.setDetail("Quantidade do item invalida.");
    } else if (message.startsWith("movimento_estoque_item_valor_unitario_invalid")) {
      pd.setDetail("Valor unitario do item invalido.");
    } else if (message.startsWith("movimento_estoque_item_tipo_nao_habilitado")) {
      pd.setDetail("Tipo de item nao habilitado para a configuracao do movimento.");
    } else if (message.startsWith("movimento_estoque_item_catalogo_invalido")) {
      pd.setDetail("Item de catalogo invalido para o contexto da empresa e tipo de item.");
    } else if (message.startsWith("movimento_estoque_item_unidade_invalid")) {
      pd.setDetail("Unidade informada invalida para o item do movimento.");
    } else if (message.startsWith("movimento_estoque_item_price_book_invalid")) {
      pd.setDetail("Tabela de preco invalida para o item do movimento.");
    } else if (message.startsWith("movimento_estoque_item_price_variant_invalid")) {
      pd.setDetail("Variacao de preco invalida para o item do movimento.");
    } else if (message.startsWith("movimento_estoque_item_price_book_required_for_variant")) {
      pd.setDetail("Informe a tabela de preco ao selecionar uma variacao de preco.");
    } else if (message.startsWith("movimento_item_tipo_id_invalid")) {
      pd.setDetail("Identificador do tipo de item invalido.");
    } else if (message.startsWith("movimento_item_tipo_not_found")) {
      pd.setDetail("Tipo de item nao encontrado.");
    } else if (message.startsWith("movimento_item_tipo_inativo")) {
      pd.setDetail("Tipo de item inativo.");
    } else if (message.startsWith("movimento_item_tipo_nome_required")) {
      pd.setDetail("Informe o nome do tipo de item.");
    } else if (message.startsWith("movimento_item_tipo_catalog_type_required")) {
      pd.setDetail("Informe o tipo de catalogo do tipo de item.");
    } else if (message.startsWith("movimento_item_tipo_nome_duplicado")) {
      pd.setDetail("Ja existe tipo de item com este nome no locatario.");
    } else if (message.startsWith("movimento_config_item_tipo_id_invalid")) {
      pd.setDetail("Tipo de item invalido na configuracao de movimento.");
    } else if (message.startsWith("movimento_config_nome_required")) {
      pd.setDetail("Informe o nome da configuracao de movimento.");
    } else if (message.startsWith("movimento_config_id_invalid")) {
      pd.setDetail("Identificador da configuracao de movimento invalido.");
    } else if (message.startsWith("movimento_config_prioridade_invalid")) {
      pd.setDetail("Prioridade invalida. Use valor maior ou igual a zero.");
    } else if (message.startsWith("movimento_config_empresa_ids_required")) {
      pd.setDetail("Informe ao menos uma empresa para a configuracao.");
    } else if (message.startsWith("movimento_config_tipos_entidade_required")) {
      pd.setDetail("Informe ao menos um tipo de entidade permitido.");
    } else if (message.startsWith("movimento_config_tipo_entidade_padrao_required")) {
      pd.setDetail("Informe o tipo de entidade padrao.");
    } else if (message.startsWith("movimento_config_tipo_entidade_padrao_invalid")) {
      pd.setDetail("Tipo de entidade padrao invalido.");
    } else if (message.startsWith("movimento_config_tipo_padrao_fora_permitidos")) {
      pd.setDetail("O tipo de entidade padrao precisa estar na lista de permitidos.");
    } else if (message.startsWith("movimento_config_empresa_invalida")) {
      pd.setDetail("Uma ou mais empresas informadas nao pertencem ao locatario atual.");
    } else if (message.startsWith("movimento_config_tipo_entidade_invalido")) {
      pd.setDetail("Um ou mais tipos de entidade informados nao pertencem ao locatario atual.");
    } else if (message.startsWith("movimento_config_empresa_id_invalid")) {
      pd.setDetail("Empresa informada para resolver configuracao e invalida.");
    } else if (message.startsWith("movimento_config_conflito_prioridade_contexto_empresa")) {
      pd.setDetail("Conflito: ja existe configuracao ativa com mesmo tipo e empresa.");
    } else if (message.startsWith("movimento_config_conflito_resolucao")) {
      pd.setDetail("Conflito: existem configuracoes empatadas para a mesma resolucao.");
    } else if (message.startsWith("movimento_config_nao_encontrada")) {
      pd.setDetail(
        "Nao existe configuracao aplicavel. Acesse Configuracoes de Movimentos, selecione o tipo e vincule a empresa.");
    } else if (message.startsWith("movimento_config_integridade_invalida")) {
      pd.setDetail("A configuracao viola restricoes de integridade dos dados.");
    } else if (message.startsWith("movimento_estoque_stock_adjustment_required")) {
      pd.setDetail("Informe o tipo de ajuste de estoque no cabecalho do movimento.");
    } else if (message.startsWith("movimento_estoque_stock_adjustment_invalid")) {
      pd.setDetail("Tipo de ajuste de estoque invalido ou inativo.");
    } else if (message.startsWith("workflow_payload_required")) {
      pd.setDetail("Payload de workflow obrigatorio.");
    } else if (message.startsWith("workflow_definition_not_draft")) {
      pd.setDetail("Somente versoes em rascunho podem ser alteradas/publicadas.");
    } else if (message.startsWith("workflow_definition_origin_immutable")) {
      pd.setDetail("A origem da definicao de workflow nao pode ser alterada.");
    } else if (message.startsWith("workflow_definition_context_immutable")) {
      pd.setDetail("O contexto da definicao de workflow nao pode ser alterado.");
    } else if (message.startsWith("workflow_definition_not_published")) {
      pd.setDetail("Nao existe workflow publicado para a origem informada.");
    } else if (message.startsWith("workflow_context_invalid")) {
      pd.setDetail("Contexto do workflow invalido.");
    } else if (message.startsWith("workflow_context_id_invalid")) {
      pd.setDetail("Identificador do contexto do workflow invalido.");
    } else if (message.startsWith("workflow_context_reference_invalid")) {
      pd.setDetail("Configuracao de contexto do workflow nao encontrada para o locatario atual.");
    } else if (message.startsWith("workflow_state_initial_exactly_one")) {
      pd.setDetail("A definicao precisa de exatamente um estado inicial.");
    } else if (message.startsWith("workflow_transition_not_found")) {
      pd.setDetail("Transicao de workflow nao encontrada.");
    } else if (message.startsWith("workflow_transition_disabled")) {
      pd.setDetail("A transicao de workflow esta desabilitada.");
    } else if (message.startsWith("workflow_transition_state_invalid")) {
      pd.setDetail("A transicao nao e valida para o estado atual.");
    } else if (message.startsWith("workflow_current_state_mismatch")) {
      pd.setDetail("O estado atual mudou. Atualize a tela e tente novamente.");
    } else if (message.startsWith("workflow_action_move_stock_origin_invalid")) {
      pd.setDetail("A acao de movimentar estoque so pode ser usada no workflow de item de movimento.");
    } else if (message.startsWith("workflow_action_undo_stock_origin_invalid")) {
      pd.setDetail("A acao de desfazer estoque so pode ser usada no workflow de item de movimento.");
    } else if (message.startsWith("workflow_action_set_item_status_origin_invalid")) {
      pd.setDetail("A acao de trocar situacao do item so pode ser usada no workflow de movimento de estoque.");
    } else if (message.startsWith("workflow_action_item_status_target_invalid")) {
      pd.setDetail("Situacao de destino invalida para os itens do movimento.");
    } else if (message.startsWith("workflow_action_json_invalid")) {
      pd.setDetail("Configuracao de acoes do workflow invalida.");
    } else if (message.startsWith("workflow_import_payload_invalid")) {
      pd.setDetail("Arquivo JSON de importacao de workflow invalido.");
    } else if (message.startsWith("workflow_json_invalid")) {
      pd.setDetail("Falha ao serializar dados do workflow.");
    } else {
      pd.setDetail(ex.getMessage());
    }
    return pd;
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    pd.setTitle("Conflito de concorrencia");
    String className = ex.getPersistentClassName() == null ? "" : ex.getPersistentClassName();
    if (className.endsWith("MovimentoEstoque")) {
      pd.setDetail("O movimento foi alterado por outro usuario. Recarregue a ficha e tente novamente.");
    } else {
      pd.setDetail("catalog_configuration_version_conflict");
    }
    return pd;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
    String normalized = message.toLowerCase();
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    pd.setTitle("Conflito de integridade");
    if (normalized.contains("ux_agrupador_empresa_item_config_empresa")) {
      pd.setDetail("Esta empresa ja esta vinculada a outro agrupador nesta configuracao.");
    } else if (normalized.contains("ux_agrupador_empresa_config_nome")) {
      pd.setDetail("Ja existe agrupador com este nome para esta configuracao.");
    } else if (normalized.contains("ux_catalog_configuration_tenant_type")) {
      pd.setDetail("Ja existe configuracao de catalogo para este tipo no locatario.");
    } else if (normalized.contains("ux_registro_entidade_codigo_scope")) {
      pd.setDetail("Ja existe entidade com este codigo na configuracao atual.");
    } else if (normalized.contains("ux_pessoa_tenant_tipo_registro_federal_norm")) {
      pd.setDetail("Ja existe pessoa com este documento neste locatario.");
    } else if (normalized.contains("ux_grupo_entidade_nome_parent_ativo")) {
      pd.setDetail("Ja existe grupo com este nome no mesmo nivel.");
    } else if (normalized.contains("ux_catalog_product_codigo_scope")
      || normalized.contains("ux_catalog_service_item_codigo_scope")) {
      pd.setDetail("Ja existe item com este codigo no escopo atual.");
    } else if (normalized.contains("ux_catalog_group_nome_parent_ativo")) {
      pd.setDetail("Ja existe grupo com este nome no mesmo nivel.");
    } else if (normalized.contains("ux_catalog_stock_balance_scope")) {
      pd.setDetail("Conflito de saldo de estoque no escopo atual. Tente novamente.");
    } else if (normalized.contains("ux_catalog_movement_idempotency")) {
      pd.setDetail("Movimentacao de estoque duplicada para a mesma chave de idempotencia.");
    } else if (normalized.contains("ux_catalog_stock_type_scope_codigo_active")) {
      pd.setDetail("Ja existe tipo de estoque com este codigo no agrupador.");
    } else if (normalized.contains("ux_catalog_stock_adjustment_scope_codigo_active")) {
      pd.setDetail("Ja existe ajuste de estoque com este codigo na configuracao.");
    } else if (normalized.contains("ux_catalog_stock_adjustment_tenant_codigo")) {
      pd.setDetail("Ja existe ajuste de estoque com este codigo na configuracao.");
    } else if (normalized.contains("ux_official_unit_codigo")) {
      pd.setDetail("Ja existe unidade oficial com este codigo.");
    } else if (normalized.contains("ux_tenant_unit_tenant_sigla")) {
      pd.setDetail("Ja existe unidade com esta sigla no locatario.");
    } else if (normalized.contains("ux_tenant_unit_conversion_scope")) {
      pd.setDetail("Ja existe conversao cadastrada para origem e destino informados.");
    } else if (normalized.contains("ux_price_book_tenant_name")) {
      pd.setDetail("Ja existe tabela de preco com este nome.");
    } else if (normalized.contains("ux_price_variant_tenant_name")) {
      pd.setDetail("Ja existe variacao de preco com este nome.");
    } else if (normalized.contains("ux_sale_price_scope")) {
      pd.setDetail("Ja existe preco para a combinacao informada.");
    } else if (normalized.contains("ux_movimento_config_empresa_scope")) {
      pd.setDetail("A empresa ja esta vinculada a esta configuracao de movimento.");
    } else if (normalized.contains("ux_movimento_config_tipo_entidade_scope")) {
      pd.setDetail("O tipo de entidade ja esta vinculado a esta configuracao de movimento.");
    } else if (normalized.contains("ux_movimento_item_tipo_tenant_nome")) {
      pd.setDetail("Ja existe tipo de item com este nome no locatario.");
    } else if (normalized.contains("ux_mov_config_item_tipo_scope")) {
      pd.setDetail("Tipo de item ja vinculado a configuracao de movimento.");
    } else if (normalized.contains("ux_movimento_estoque_item_codigo_scope")) {
      pd.setDetail("Conflito de codigo sequencial de item no movimento. Recarregue a ficha e tente novamente.");
    } else if (normalized.contains("ux_movimento_estoque_codigo_scope")) {
      pd.setDetail("Conflito de codigo sequencial do movimento. Recarregue a lista e tente novamente.");
    } else if (normalized.contains("ux_workflow_def_tenant_origin_ctx_version")
      || normalized.contains("ux_workflow_def_tenant_origin_version")) {
      pd.setDetail("Ja existe versao de workflow com este numero para a origem.");
    } else if (normalized.contains("ux_workflow_def_tenant_origin_ctx_published")
      || normalized.contains("ux_workflow_def_tenant_origin_published")) {
      pd.setDetail("Ja existe workflow publicado ativo para esta origem.");
    } else if (normalized.contains("ux_workflow_state_def_key")) {
      pd.setDetail("Chave de estado duplicada na mesma definicao.");
    } else if (normalized.contains("ux_workflow_transition_def_key")) {
      pd.setDetail("Chave de transicao duplicada na mesma definicao.");
    } else if (normalized.contains("ux_workflow_instance_origin_entity")) {
      pd.setDetail("Instancia de workflow ja existe para este registro.");
    } else if (normalized.contains("ux_wf_action_exec_key")) {
      pd.setDetail("Execucao de acao de workflow duplicada (idempotencia).");
    } else if (normalized.contains("ux_mov_item_tenant_mov_chave")) {
      pd.setDetail("Item ja possui movimentacao de estoque com esta chave de idempotencia.");
    } else {
      pd.setDetail("Operacao violou uma restricao de integridade.");
    }
    return pd;
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGeneric(Exception ex) {
    Throwable root = rootCause(ex);
    if (root instanceof IllegalArgumentException iae) {
      return handleIllegalArgument(iae);
    }
    if (root instanceof EntityNotFoundException enfe) {
      return handleNotFound(enfe);
    }
    if (root instanceof IllegalStateException ise) {
      return handleIllegalState(ise);
    }
    if (root instanceof AccessDeniedException ade) {
      return handleAccessDenied(ade);
    }
    if (root instanceof ObjectOptimisticLockingFailureException oole) {
      return handleOptimisticLock(oole);
    }
    if (root instanceof DataIntegrityViolationException dive) {
      return handleDataIntegrityViolation(dive);
    }

    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    pd.setTitle("Erro interno");
    pd.setDetail("Erro inesperado ao processar a requisicao.");
    return pd;
  }

  private boolean isConflict(String message) {
    return message.startsWith("usuario_email_duplicado")
      || message.startsWith("usuario_username_duplicado")
      || message.startsWith("agrupador_nome_duplicado")
      || message.startsWith("empresa_ja_vinculada_outro_agrupador")
      || message.startsWith("tipo_entidade_nome_duplicado")
      || message.startsWith("tipo_entidade_padrao_nao_excluivel")
      || message.startsWith("tipo_entidade_padrao_inativacao_nao_permitida")
      || message.startsWith("tipo_entidade_config_duplicada_agrupador")
      || message.startsWith("empresa_context_required")
      || message.startsWith("empresa_sem_grupo_no_tipo_entidade")
      || message.startsWith("entidade_codigo_duplicado_configuracao")
      || message.startsWith("pessoa_registro_federal_duplicado")
      || message.startsWith("grupo_entidade_nome_duplicado_mesmo_pai")
      || message.startsWith("grupo_entidade_possui_entidades")
      || message.startsWith("grupo_entidade_ciclo_invalido")
      || message.startsWith("catalog_configuration_version_conflict")
      || message.startsWith("catalog_configuration_group_duplicated")
      || message.startsWith("catalog_context_required")
      || message.startsWith("catalog_context_sem_grupo")
      || message.startsWith("catalog_item_codigo_duplicado")
      || message.startsWith("price_book_name_duplicated")
      || message.startsWith("price_variant_name_duplicated")
      || message.startsWith("sale_price_scope_duplicated")
      || message.startsWith("catalog_item_unit_locked_by_stock_movements")
      || message.startsWith("catalog_group_nome_duplicado_mesmo_pai")
      || message.startsWith("catalog_group_possui_itens")
      || message.startsWith("catalog_group_ciclo_invalido")
      || message.startsWith("official_unit_codigo_duplicado")
      || message.startsWith("official_unit_codigo_immutable")
      || message.startsWith("official_unit_in_use")
      || message.startsWith("tenant_unit_sigla_duplicada")
      || message.startsWith("tenant_unit_mirror_delete_not_allowed")
      || message.startsWith("tenant_unit_in_use")
      || message.startsWith("tenant_unit_conversion_duplicada")
      || message.startsWith("catalog_stock_type_codigo_duplicado")
      || message.startsWith("catalog_stock_type_last_active")
      || message.startsWith("catalog_stock_adjustment_codigo_duplicado")
      || message.startsWith("movimento_item_tipo_nome_duplicado")
      || message.startsWith("movimento_config_conflito_prioridade_contexto_empresa")
      || message.startsWith("movimento_config_conflito_resolucao")
      || message.startsWith("movimento_estoque_finalizado")
      || message.startsWith("movimento_estoque_item_finalizado")
      || message.startsWith("movimento_estoque_item_locked_by_stock_movement")
      || message.startsWith("workflow_current_state_mismatch");
  }

  private Throwable rootCause(Throwable ex) {
    Throwable current = ex;
    while (current != null && current.getCause() != null && current.getCause() != current) {
      current = current.getCause();
    }
    return current == null ? ex : current;
  }
}
