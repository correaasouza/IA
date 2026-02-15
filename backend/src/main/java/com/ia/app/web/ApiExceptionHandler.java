package com.ia.app.web;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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
    } else {
      pd.setDetail(ex.getMessage());
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
      || message.startsWith("tipo_entidade_config_duplicada_agrupador");
  }
}
