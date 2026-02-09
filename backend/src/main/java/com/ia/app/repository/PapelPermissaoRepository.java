package com.ia.app.repository;

import com.ia.app.domain.PapelPermissao;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PapelPermissaoRepository extends JpaRepository<PapelPermissao, Long> {
  List<PapelPermissao> findAllByTenantIdAndPapelId(Long tenantId, Long papelId);
  void deleteAllByTenantIdAndPapelId(Long tenantId, Long papelId);

  @Query("select p.permissaoCodigo from PapelPermissao p where p.tenantId = :tenantId and p.papelId in :papelIds")
  List<String> findPermissoesByPapelIds(@Param("tenantId") Long tenantId, @Param("papelIds") List<Long> papelIds);
}
