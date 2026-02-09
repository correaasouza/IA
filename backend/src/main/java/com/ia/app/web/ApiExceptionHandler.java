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
    ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    pd.setTitle("Requisição inválida");
    pd.setDetail(ex.getMessage());
    return pd;
  }
}
