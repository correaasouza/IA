package com.ia.app.repository;

import com.ia.app.domain.UsuarioEmpresaAcesso;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioEmpresaAcessoRepository extends JpaRepository<UsuarioEmpresaAcesso, Long> {

  @Query("select a.empresaId from UsuarioEmpresaAcesso a where a.tenantId = :tenantId and a.usuarioId = :usuarioId order by a.empresaId asc")
  List<Long> findEmpresaIdsByTenantIdAndUsuarioId(@Param("tenantId") Long tenantId, @Param("usuarioId") String usuarioId);

  @Modifying
  @Query("delete from UsuarioEmpresaAcesso a where a.tenantId = :tenantId and a.usuarioId = :usuarioId")
  void deleteAllByTenantIdAndUsuarioId(@Param("tenantId") Long tenantId, @Param("usuarioId") String usuarioId);

  @Modifying
  @Query("delete from UsuarioEmpresaAcesso a where a.usuarioId = :usuarioId")
  void deleteAllByUsuarioId(@Param("usuarioId") String usuarioId);
}
