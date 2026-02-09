package com.ia.app.repository;

import com.ia.app.domain.Locatario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface LocatarioRepository extends JpaRepository<Locatario, Long>, JpaSpecificationExecutor<Locatario> {

  @Query("select count(l.id) from Locatario l")
  long countTotal();

  @Query("select count(l.id) from Locatario l where l.ativo = true and l.dataLimiteAcesso >= current_date")
  long countAtivos();

  @Query("select count(l.id) from Locatario l where l.ativo = false or l.dataLimiteAcesso < current_date")
  long countBloqueados();
}
