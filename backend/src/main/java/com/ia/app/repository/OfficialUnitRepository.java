package com.ia.app.repository;

import com.ia.app.domain.OfficialUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficialUnitRepository extends JpaRepository<OfficialUnit, UUID> {

  List<OfficialUnit> findAllByOrderByCodigoOficialAsc();

  List<OfficialUnit> findAllByAtivoTrueOrderByCodigoOficialAsc();

  Optional<OfficialUnit> findByCodigoOficialIgnoreCase(String codigoOficial);

  boolean existsByCodigoOficialIgnoreCase(String codigoOficial);
}
