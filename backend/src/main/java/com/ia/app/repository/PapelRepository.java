package com.ia.app.repository;

import com.ia.app.domain.Papel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PapelRepository extends JpaRepository<Papel, Long> {
  List<Papel> findAllByTenantIdOrderByNome(Long tenantId);
  Optional<Papel> findByTenantIdAndNome(Long tenantId, String nome);
}
