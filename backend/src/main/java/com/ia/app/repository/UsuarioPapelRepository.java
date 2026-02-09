package com.ia.app.repository;

import com.ia.app.domain.UsuarioPapel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioPapelRepository extends JpaRepository<UsuarioPapel, Long> {
  List<UsuarioPapel> findAllByTenantIdAndUsuarioId(Long tenantId, String usuarioId);
  List<UsuarioPapel> findAllByTenantIdAndUsuarioIdIn(Long tenantId, List<String> usuarioIds);
  void deleteAllByTenantIdAndUsuarioId(Long tenantId, String usuarioId);

  @Query("select up.papelId from UsuarioPapel up where up.tenantId = :tenantId and up.usuarioId = :usuarioId")
  List<Long> findPapelIdsByUsuario(@Param("tenantId") Long tenantId, @Param("usuarioId") String usuarioId);
}
