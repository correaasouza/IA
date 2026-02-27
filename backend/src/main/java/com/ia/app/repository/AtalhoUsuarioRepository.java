package com.ia.app.repository;

import com.ia.app.domain.AtalhoUsuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtalhoUsuarioRepository extends JpaRepository<AtalhoUsuario, Long> {
  List<AtalhoUsuario> findAllByTenantIdAndUserIdOrderByOrdemAsc(Long tenantId, String userId);
  Optional<AtalhoUsuario> findByTenantIdAndUserIdAndMenuId(Long tenantId, String userId, String menuId);
  Optional<AtalhoUsuario> findByIdAndTenantIdAndUserId(Long id, Long tenantId, String userId);
  void deleteAllByUserId(String userId);
}
