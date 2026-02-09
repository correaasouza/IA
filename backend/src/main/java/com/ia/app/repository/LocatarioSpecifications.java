package com.ia.app.repository;

import com.ia.app.domain.Locatario;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public class LocatarioSpecifications {
  private LocatarioSpecifications() {}

  public static Specification<Locatario> nomeLike(String nome) {
    return (root, query, cb) -> {
      if (nome == null || nome.isBlank()) {
        return cb.conjunction();
      }
      return cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%");
    };
  }

  public static Specification<Locatario> ativoEquals(Boolean ativo) {
    return (root, query, cb) -> {
      if (ativo == null) {
        return cb.conjunction();
      }
      return cb.equal(root.get("ativo"), ativo);
    };
  }

  public static Specification<Locatario> bloqueadoEquals(Boolean bloqueado) {
    return (root, query, cb) -> {
      if (bloqueado == null) {
        return cb.conjunction();
      }
      LocalDate today = LocalDate.now();
      if (bloqueado) {
        return cb.or(
          cb.lessThan(root.get("dataLimiteAcesso"), today),
          cb.isFalse(root.get("ativo"))
        );
      }
      return cb.and(
        cb.greaterThanOrEqualTo(root.get("dataLimiteAcesso"), today),
        cb.isTrue(root.get("ativo"))
      );
    };
  }
}
