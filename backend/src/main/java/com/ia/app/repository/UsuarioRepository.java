package com.ia.app.repository;

import com.ia.app.domain.Usuario;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
  Page<Usuario> findAllByTenantId(Long tenantId, Pageable pageable);
  Optional<Usuario> findByIdAndTenantId(Long id, Long tenantId);
  Optional<Usuario> findByTenantIdAndUsername(Long tenantId, String username);
  Optional<Usuario> findByKeycloakIdAndTenantId(String keycloakId, Long tenantId);
  Optional<Usuario> findByKeycloakId(String keycloakId);
}
