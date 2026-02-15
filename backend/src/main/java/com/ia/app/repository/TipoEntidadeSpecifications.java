package com.ia.app.repository;

import com.ia.app.domain.TipoEntidade;
import org.springframework.data.jpa.domain.Specification;

public class TipoEntidadeSpecifications {
  private TipoEntidadeSpecifications() {}

  public static Specification<TipoEntidade> tenantEquals(Long tenantId) {
    return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
  }

  public static Specification<TipoEntidade> nomeLike(String nome) {
    return (root, query, cb) -> {
      if (nome == null || nome.isBlank()) {
        return cb.conjunction();
      }
      return cb.like(cb.lower(root.get("nome")), "%" + nome.toLowerCase() + "%");
    };
  }

  public static Specification<TipoEntidade> ativoEquals(Boolean ativo) {
    return (root, query, cb) -> {
      if (ativo == null) {
        return cb.conjunction();
      }
      return cb.equal(root.get("ativo"), ativo);
    };
  }
}
