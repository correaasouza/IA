package com.ia.app.web;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(EntityNotFoundException.class)
  public ProblemDetail handleNotFound(EntityNotFoundException ex) {
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    pd.setTitle("Recurso não encontrado");
    pd.setDetail(ex.getMessage());
    return pd;
  }

  @ExceptionHandler(IllegalStateException.class)
  public ProblemDetail handleIllegalState(IllegalStateException ex) {
    HttpStatus status = ex.getMessage() != null && ex.getMessage().startsWith("role_required_")
      ? HttpStatus.FORBIDDEN
      : HttpStatus.BAD_REQUEST;
    ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setTitle("Requisição inválida");
    pd.setDetail(ex.getMessage());
    return pd;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    String message = ex.getMessage() == null ? "" : ex.getMessage();
    HttpStatus status = (message.startsWith("usuario_email_duplicado")
      || message.startsWith("usuario_username_duplicado"))
      ? HttpStatus.CONFLICT
      : HttpStatus.BAD_REQUEST;
    ProblemDetail pd = ProblemDetail.forStatus(status);
    pd.setTitle("Requisição inválida");
    if (message.startsWith("usuario_email_duplicado")) {
      pd.setDetail("E-mail já cadastrado para outro usuário.");
    } else if (message.startsWith("usuario_username_duplicado")) {
      pd.setDetail("Username já cadastrado para outro usuário.");
    } else {
      pd.setDetail(ex.getMessage());
    }
    return pd;
  }
}

