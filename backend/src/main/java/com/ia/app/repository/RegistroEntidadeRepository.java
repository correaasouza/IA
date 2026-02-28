package com.ia.app.repository;

import com.ia.app.domain.RegistroEntidade;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistroEntidadeRepository extends JpaRepository<RegistroEntidade, Long> {

  interface GrupoEntidadeCountRow {
    Long getGrupoId();
    Long getTotal();
  }

  Optional<RegistroEntidade> findByIdAndTenantIdAndTipoEntidadeConfigAgrupadorIdAndAtivoTrue(
    Long id, Long tenantId, Long tipoEntidadeConfigAgrupadorId);

  Optional<RegistroEntidade> findByIdAndTenantIdAndTipoEntidadeConfigAgrupadorId(
    Long id, Long tenantId, Long tipoEntidadeConfigAgrupadorId);

  boolean existsByTenantIdAndTipoEntidadeConfigAgrupadorIdAndGrupoEntidadeIdInAndAtivoTrue(
    Long tenantId, Long tipoEntidadeConfigAgrupadorId, Collection<Long> grupoEntidadeIds);

  @Query("""
    select re.grupoEntidadeId as grupoId, count(re.id) as total
    from RegistroEntidade re
    where re.tenantId = :tenantId
      and re.tipoEntidadeConfigAgrupadorId = :configId
      and re.ativo = true
      and re.grupoEntidadeId is not null
    group by re.grupoEntidadeId
    """)
  List<GrupoEntidadeCountRow> countAtivosByGrupo(
    @Param("tenantId") Long tenantId,
    @Param("configId") Long tipoEntidadeConfigAgrupadorId);

  @Query(
    value = """
      SELECT re.*
      FROM registro_entidade re
      JOIN pessoa p
        ON p.id = re.pessoa_id
       AND p.tenant_id = re.tenant_id
      WHERE re.tenant_id = :tenantId
        AND re.empresa_id = :empresaId
        AND re.tipo_entidade_config_agrupador_id = :configId
        AND (
          (:codigo IS NULL AND :pessoaNome IS NULL AND :registroFederalNorm IS NULL)
          OR (:codigo IS NOT NULL AND re.codigo = :codigo)
          OR (:pessoaNome IS NOT NULL AND lower(p.nome) LIKE lower(concat('%', :pessoaNome, '%')))
          OR (:registroFederalNorm IS NOT NULL AND p.registro_federal_normalizado = :registroFederalNorm)
        )
        AND (:grupoId IS NULL OR re.grupo_entidade_id = :grupoId)
        AND (:ativo IS NULL OR re.ativo = :ativo)
      ORDER BY re.codigo ASC
      """,
    countQuery = """
      SELECT COUNT(1)
      FROM registro_entidade re
      JOIN pessoa p
        ON p.id = re.pessoa_id
       AND p.tenant_id = re.tenant_id
      WHERE re.tenant_id = :tenantId
        AND re.empresa_id = :empresaId
        AND re.tipo_entidade_config_agrupador_id = :configId
        AND (
          (:codigo IS NULL AND :pessoaNome IS NULL AND :registroFederalNorm IS NULL)
          OR (:codigo IS NOT NULL AND re.codigo = :codigo)
          OR (:pessoaNome IS NOT NULL AND lower(p.nome) LIKE lower(concat('%', :pessoaNome, '%')))
          OR (:registroFederalNorm IS NOT NULL AND p.registro_federal_normalizado = :registroFederalNorm)
        )
        AND (:grupoId IS NULL OR re.grupo_entidade_id = :grupoId)
        AND (:ativo IS NULL OR re.ativo = :ativo)
      """,
    nativeQuery = true)
  Page<RegistroEntidade> search(
    @Param("tenantId") Long tenantId,
    @Param("empresaId") Long empresaId,
    @Param("configId") Long tipoEntidadeConfigAgrupadorId,
    @Param("codigo") Long codigo,
    @Param("pessoaNome") String pessoaNome,
    @Param("registroFederalNorm") String registroFederalNorm,
    @Param("grupoId") Long grupoId,
    @Param("ativo") Boolean ativo,
    Pageable pageable);
}
