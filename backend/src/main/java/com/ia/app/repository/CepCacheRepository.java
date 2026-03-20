package com.ia.app.repository;

import com.ia.app.domain.CepCache;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CepCacheRepository extends JpaRepository<CepCache, Long> {
  Optional<CepCache> findByCep(String cep);
}
