package com.ia.app.repository;

import com.ia.app.domain.Usuario;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
  Page<Usuario> findAllByTenantId(Long tenantId, Pageable pageable);
  @Query(
      value = """
          select u from Usuario u
          where u.tenantId = :tenantId
             or exists (
               select 1 from UsuarioLocatarioAcesso a
               where a.usuarioId = u.keycloakId
                 and a.locatarioId = :tenantId
             )
          """,
      countQuery = """
          select count(u) from Usuario u
          where u.tenantId = :tenantId
             or exists (
               select 1 from UsuarioLocatarioAcesso a
               where a.usuarioId = u.keycloakId
                 and a.locatarioId = :tenantId
             )
          """
  )
  Page<Usuario> findAllAccessibleByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);
  Optional<Usuario> findByIdAndTenantId(Long id, Long tenantId);
  Optional<Usuario> findByTenantIdAndUsername(Long tenantId, String username);
  boolean existsByUsernameIgnoreCase(String username);
  boolean existsByEmailIgnoreCase(String email);
  Optional<Usuario> findByKeycloakIdAndTenantId(String keycloakId, Long tenantId);
  Optional<Usuario> findByKeycloakId(String keycloakId);
}
