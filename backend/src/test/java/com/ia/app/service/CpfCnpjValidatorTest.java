package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ia.app.util.CpfCnpjValidator;
import org.junit.jupiter.api.Test;

class CpfCnpjValidatorTest {

  @Test
  void shouldRejectInvalidCpf() {
    assertThatThrownBy(() -> {
      if (!CpfCnpjValidator.isValid("123.456.789-00")) {
        throw new IllegalArgumentException("invalid");
      }
    }).isInstanceOf(IllegalArgumentException.class);
  }
}
