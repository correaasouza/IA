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
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    pd.setTitle("Recurso nao encontrado");
    pd.setDetail(ex.getMessage());
    return pd;
  }

  @ExceptionHandler(IllegalStateException.class)
  public ProblemDetail handleIllegalState(IllegalStateException ex) {
    String message = ex.getMessage() == null ? "" : ex.getMessage();
    HttpStatus status = message.startsWith("role_required_")
      ? HttpStatus.FORBIDDEN
      : message.equals("unauthorized")
        ? HttpStatus.UNAUTHORIZED
        : HttpStatus.BAD_REQUEST;
    ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setTitle("Requisicao invalida");
    pd.setDetail(ex.getMessage());
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
      || message.startsWith("catalog_group_ciclo_invalido");
  }
}
