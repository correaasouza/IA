package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ia.app.domain.Locatario;
import com.ia.app.repository.LocatarioRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class LocatarioRepositoryTest {

  @Autowired
  private LocatarioRepository repository;

  @Test
  void shouldSaveLocatario() {
    Locatario loc = new Locatario();
    loc.setNome("Teste");
    loc.setDataLimiteAcesso(LocalDate.now().plusDays(10));
    loc.setAtivo(true);
    Locatario saved = repository.save(loc);
    assertThat(saved.getId()).isNotNull();
  }
}
