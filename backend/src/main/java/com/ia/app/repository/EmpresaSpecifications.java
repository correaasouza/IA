package com.ia.app.repository;

import com.ia.app.domain.Empresa;
import org.springframework.data.jpa.domain.Specification;

public class EmpresaSpecifications {
  private EmpresaSpecifications() {}

  public static Specification<Empresa> tenantEquals(Long tenantId) {
    return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
  }

  public static Specification<Empresa> nomeLike(String nome) {
    return (root, query, cb) -> {
      if (nome == null || nome.isBlank()) {
        return cb.conjunction();
      }
      return cb.like(cb.lower(root.get("razaoSocial")), "%" + nome.toLowerCase() + "%");
    };
  }

  public static Specification<Empresa> cnpjLike(String cnpj) {
    return (root, query, cb) -> {
      if (cnpj == null || cnpj.isBlank()) {
        return cb.conjunction();
      }
      return cb.like(root.get("cnpj"), "%" + cnpj + "%");
    };
  }

  public static Specification<Empresa> tipoEquals(String tipo) {
    return (root, query, cb) -> {
      if (tipo == null || tipo.isBlank()) {
        return cb.conjunction();
      }
      return cb.equal(root.get("tipo"), tipo.trim().toUpperCase());
    };
  }

  public static Specification<Empresa> matrizEquals(Long matrizId) {
    return (root, query, cb) -> {
      if (matrizId == null) {
        return cb.conjunction();
      }
      return cb.equal(root.get("matriz").get("id"), matrizId);
    };
  }

  public static Specification<Empresa> ativoEquals(Boolean ativo) {
    return (root, query, cb) -> {
      if (ativo == null) {
        return cb.conjunction();
      }
      return cb.equal(root.get("ativo"), ativo);
    };
  }
}
