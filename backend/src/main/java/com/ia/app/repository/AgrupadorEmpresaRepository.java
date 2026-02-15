package com.ia.app.repository;

import com.ia.app.domain.AgrupadorEmpresa;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgrupadorEmpresaRepository extends JpaRepository<AgrupadorEmpresa, Long> {
  @EntityGraph(attributePaths = "itens")
  List<AgrupadorEmpresa> findAllByTenantIdAndConfigTypeAndConfigIdAndAtivoTrueOrderByNomeAsc(
    Long tenantId, String configType, Long configId);

  @EntityGraph(attributePaths = "itens")
  Optional<AgrupadorEmpresa> findByIdAndTenantIdAndConfigTypeAndConfigIdAndAtivoTrue(
    Long id, Long tenantId, String configType, Long configId);

  boolean existsByTenantIdAndConfigTypeAndConfigIdAndNomeIgnoreCaseAndAtivoTrue(
    Long tenantId, String configType, Long configId, String nome);

  boolean existsByTenantIdAndConfigTypeAndConfigIdAndNomeIgnoreCaseAndAtivoTrueAndIdNot(
    Long tenantId, String configType, Long configId, String nome, Long id);
}
