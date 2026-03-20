package com.ia.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ia.app.domain.Pessoa;
import com.ia.app.domain.RegistroEntidade;
import com.ia.app.dto.PessoaVinculoRequest;
import com.ia.app.dto.RegistroEntidadeRequest;
import com.ia.app.dto.RegistroEntidadeResponse;
import com.ia.app.repository.GrupoEntidadeRepository;
import com.ia.app.repository.PessoaRepository;
import com.ia.app.repository.PriceBookRepository;
import com.ia.app.repository.RegistroEntidadeRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RegistroEntidadeServiceTest {

  @Mock
  private RegistroEntidadeRepository repository;

  @Mock
  private RegistroEntidadeContextoService contextoService;

  @Mock
  private RegistroEntidadeCodigoService codigoService;

  @Mock
  private PessoaResolveService pessoaResolveService;

  @Mock
  private PessoaRepository pessoaRepository;

  @Mock
  private GrupoEntidadeRepository grupoRepository;

  @Mock
  private PriceBookRepository priceBookRepository;

  @Mock
  private AuditService auditService;

  @InjectMocks
  private RegistroEntidadeService service;

  @Test
  void shouldUpdateLinkedPessoaTogetherWithEntity() {
    Long tenantId = 77L;
    Long empresaId = 88L;
    Long tipoEntidadeId = 9L;
    Long configAgrupadorId = 11L;
    Long entidadeId = 100L;
    Long pessoaAtualId = 200L;

    var scope = new RegistroEntidadeContextoService.RegistroEntidadeScope(
      tenantId, empresaId, tipoEntidadeId, 5L, "Grupo", configAgrupadorId);
    when(contextoService.resolveObrigatorio(tipoEntidadeId)).thenReturn(scope);

    RegistroEntidade entity = new RegistroEntidade();
    ReflectionTestUtils.setField(entity, "id", entidadeId);
    entity.setTenantId(tenantId);
    entity.setEmpresaId(empresaId);
    entity.setTipoEntidadeConfigAgrupadorId(configAgrupadorId);
    entity.setCodigo(1234L);
    entity.setPessoaId(pessoaAtualId);
    entity.setAtivo(true);

    when(repository.findByIdAndTenantIdAndTipoEntidadeConfigAgrupadorId(entidadeId, tenantId, configAgrupadorId))
      .thenReturn(Optional.of(entity));
    when(repository.save(any(RegistroEntidade.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Pessoa pessoaAtualizada = new Pessoa();
    ReflectionTestUtils.setField(pessoaAtualizada, "id", pessoaAtualId);
    pessoaAtualizada.setTenantId(tenantId);
    pessoaAtualizada.setNome("Pessoa Atualizada");
    pessoaAtualizada.setApelido("Apelido Atualizado");
    pessoaAtualizada.setTipoRegistro("CPF");
    pessoaAtualizada.setRegistroFederal("39053344705");
    pessoaAtualizada.setRegistroFederalNormalizado("39053344705");
    pessoaAtualizada.setTipoPessoa("FISICA");
    pessoaAtualizada.setAtivo(true);

    var pessoaRequest = new PessoaVinculoRequest(
      "Pessoa Atualizada",
      "Apelido Atualizado",
      "CPF",
      "390.533.447-05",
      null,
      null,
      null,
      null,
      null,
      null);
    when(pessoaResolveService.updateLinkedPessoa(tenantId, pessoaAtualId, pessoaRequest))
      .thenReturn(pessoaAtualizada);
    when(pessoaRepository.findByIdAndTenantId(pessoaAtualId, tenantId)).thenReturn(Optional.of(pessoaAtualizada));

    RegistroEntidadeRequest request = new RegistroEntidadeRequest(
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      "Sr",
      null,
      true,
      pessoaRequest);

    RegistroEntidadeResponse response = service.update(tipoEntidadeId, entidadeId, request);

    verify(pessoaResolveService).updateLinkedPessoa(tenantId, pessoaAtualId, pessoaRequest);
    verify(pessoaResolveService, never()).resolveOrCreate(any(), any());

    ArgumentCaptor<RegistroEntidade> captor = ArgumentCaptor.forClass(RegistroEntidade.class);
    verify(repository).save(captor.capture());
    RegistroEntidade saved = captor.getValue();
    assertThat(saved.getPessoaId()).isEqualTo(pessoaAtualId);
    assertThat(saved.getTratamento()).isEqualTo("Sr");

    assertThat(response.id()).isEqualTo(entidadeId);
    assertThat(response.pessoa().id()).isEqualTo(pessoaAtualId);
    assertThat(response.pessoa().nome()).isEqualTo("Pessoa Atualizada");
    assertThat(response.pessoa().apelido()).isEqualTo("Apelido Atualizado");
    assertThat(response.pessoa().tipoRegistro()).isEqualTo("CPF");
    assertThat(response.pessoa().registroFederal()).isEqualTo("39053344705");
  }
}
