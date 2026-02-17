package com.ia.app.web;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
    } else if (message.startsWith("registro_entidade_not_found")) {
      pd.setDetail("Entidade nao encontrada no contexto atual (empresa/grupo).");
    } else if (message.startsWith("movimento_config_not_found")) {
      pd.setDetail("Configuracao de movimento nao encontrada no locatario atual.");
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
          ? HttpStatus.SERVICE_UNAVAILABLE
        : HttpStatus.BAD_REQUEST;
    ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setTitle("Requisicao invalida");
    if (message.startsWith("movimento_config_feature_disabled")) {
      pd.setDetail("Modulo de Configuracoes de Movimentos desabilitado por feature flag.");
    } else {
      pd.setDetail(ex.getMessage());
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
    } else if (message.startsWith("catalog_stock_metric_invalid")) {
      pd.setDetail("Metrica de movimentacao invalida. Use QUANTIDADE ou PRECO.");
    } else if (message.startsWith("catalog_stock_origin_invalid")) {
      pd.setDetail("Origem de movimentacao invalida.");
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
    } else if (message.startsWith("movimento_tipo_invalid")) {
      pd.setDetail("Tipo de movimento invalido.");
    } else if (message.startsWith("movimento_config_nome_required")) {
      pd.setDetail("Informe o nome da configuracao de movimento.");
    } else if (message.startsWith("movimento_config_prioridade_invalid")) {
      pd.setDetail("Prioridade invalida. Use valor maior ou igual a zero.");
    } else if (message.startsWith("movimento_config_empresa_ids_required")) {
      pd.setDetail("Informe ao menos uma empresa para a configuracao.");
    } else if (message.startsWith("movimento_config_tipos_entidade_required")) {
      pd.setDetail("Informe ao menos um tipo de entidade permitido.");
    } else if (message.startsWith("movimento_config_tipo_entidade_padrao_required")) {
      pd.setDetail("Informe o tipo de entidade padrao.");
    } else if (message.startsWith("movimento_config_tipo_padrao_fora_permitidos")) {
      pd.setDetail("O tipo de entidade padrao precisa estar na lista de permitidos.");
    } else if (message.startsWith("movimento_config_empresa_invalida")) {
      pd.setDetail("Uma ou mais empresas informadas nao pertencem ao locatario atual.");
    } else if (message.startsWith("movimento_config_tipo_entidade_invalido")) {
      pd.setDetail("Um ou mais tipos de entidade informados nao pertencem ao locatario atual.");
    } else if (message.startsWith("movimento_config_empresa_id_invalid")) {
      pd.setDetail("Empresa informada para resolver configuracao e invalida.");
    } else if (message.startsWith("movimento_config_conflito_prioridade_contexto_empresa")) {
      pd.setDetail("Conflito: ja existe configuracao ativa com mesmo tipo, contexto e empresa.");
    } else if (message.startsWith("movimento_config_conflito_resolucao")) {
      pd.setDetail("Conflito: existem configuracoes empatadas para a mesma resolucao.");
    } else if (message.startsWith("movimento_config_nao_encontrada")) {
      pd.setDetail(
        "Nao existe configuracao aplicavel. Acesse Configuracoes de Movimentos, selecione o tipo e vincule a empresa.");
    } else if (message.startsWith("movimento_config_integridade_invalida")) {
      pd.setDetail("A configuracao viola restricoes de integridade dos dados.");
    } else {
      pd.setDetail(ex.getMessage());
    }
    return pd;
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    pd.setTitle("Conflito de concorrencia");
    pd.setDetail("catalog_configuration_version_conflict");
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
    } else if (normalized.contains("ux_movimento_config_empresa_scope")) {
      pd.setDetail("A empresa ja esta vinculada a esta configuracao de movimento.");
    } else if (normalized.contains("ux_movimento_config_tipo_entidade_scope")) {
      pd.setDetail("O tipo de entidade ja esta vinculado a esta configuracao de movimento.");
    } else {
      pd.setDetail("Operacao violou uma restricao de integridade.");
    }
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
      || message.startsWith("catalog_group_nome_duplicado_mesmo_pai")
      || message.startsWith("catalog_group_possui_itens")
      || message.startsWith("catalog_group_ciclo_invalido")
      || message.startsWith("catalog_stock_type_codigo_duplicado")
      || message.startsWith("catalog_stock_type_last_active")
      || message.startsWith("catalog_stock_adjustment_codigo_duplicado")
      || message.startsWith("movimento_config_conflito_prioridade_contexto_empresa")
      || message.startsWith("movimento_config_conflito_resolucao");
  }
}
