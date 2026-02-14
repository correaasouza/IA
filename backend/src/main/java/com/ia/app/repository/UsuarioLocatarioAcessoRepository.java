package com.ia.app.repository;

import com.ia.app.domain.UsuarioLocatarioAcesso;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioLocatarioAcessoRepository extends JpaRepository<UsuarioLocatarioAcesso, Long> {

  List<UsuarioLocatarioAcesso> findAllByUsuarioId(String usuarioId);

  void deleteAllByUsuarioId(String usuarioId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from UsuarioLocatarioAcesso a where a.usuarioId = :usuarioId")
  void deleteAllByUsuarioIdDirect(@Param("usuarioId") String usuarioId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(value = """
      insert into usuario_locatario_acesso (usuario_id, locatario_id, created_at, updated_at)
      values (:usuarioId, :locatarioId, now(), now())
      on conflict (usuario_id, locatario_id) do nothing
      """, nativeQuery = true)
  void insertIgnore(@Param("usuarioId") String usuarioId, @Param("locatarioId") Long locatarioId);

  boolean existsByUsuarioIdAndLocatarioId(String usuarioId, Long locatarioId);

  @Query("select a.locatarioId from UsuarioLocatarioAcesso a where a.usuarioId = :usuarioId")
  List<Long> findLocatarioIdsByUsuarioId(@Param("usuarioId") String usuarioId);
}
