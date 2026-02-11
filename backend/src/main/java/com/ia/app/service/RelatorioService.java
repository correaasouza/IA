package com.ia.app.service;

import com.ia.app.domain.ContatoTipo;
import com.ia.app.domain.Entidade;
import com.ia.app.dto.RelatorioContatoResponse;
import com.ia.app.dto.RelatorioEntidadeResponse;
import com.ia.app.dto.RelatorioEntidadeComparativoResponse;
import com.ia.app.dto.RelatorioLocatarioStatusResponse;
import com.ia.app.dto.RelatorioPendenciaContatoResponse;
import com.ia.app.repository.ContatoTipoRepository;
import com.ia.app.repository.EntidadeRepository;
import com.ia.app.repository.LocatarioRepository;
import com.ia.app.repository.PessoaContatoRepository;
import com.ia.app.repository.PessoaRepository;
import com.ia.app.repository.TipoEntidadeRepository;
import com.ia.app.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RelatorioService {

  private final EntidadeRepository entidadeRepository;
  private final TipoEntidadeRepository tipoEntidadeRepository;
  private final PessoaContatoRepository pessoaContatoRepository;
  private final PessoaRepository pessoaRepository;
  private final LocatarioRepository locatarioRepository;
  private final ContatoTipoRepository contatoTipoRepository;

  public RelatorioService(EntidadeRepository entidadeRepository,
      TipoEntidadeRepository tipoEntidadeRepository,
      PessoaContatoRepository pessoaContatoRepository,
      PessoaRepository pessoaRepository,
      LocatarioRepository locatarioRepository,
      ContatoTipoRepository contatoTipoRepository) {
    this.entidadeRepository = entidadeRepository;
    this.tipoEntidadeRepository = tipoEntidadeRepository;
    this.pessoaContatoRepository = pessoaContatoRepository;
    this.pessoaRepository = pessoaRepository;
    this.locatarioRepository = locatarioRepository;
    this.contatoTipoRepository = contatoTipoRepository;
  }

  public List<RelatorioEntidadeResponse> entidadesPorTipo() {
    Long tenantId = requireTenant();
    Map<Long, String> nomes = tipoEntidadeRepository.findAllByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged())
      .stream().collect(Collectors.toMap(e -> e.getId(), e -> e.getNome()));

    return entidadeRepository.countByTipoEntidade(tenantId).stream().map(row -> {
      Long id = (Long) row[0];
      long total = (Long) row[1];
      return new RelatorioEntidadeResponse(id, nomes.getOrDefault(id, ""), total);
    }).toList();
  }

  public List<RelatorioEntidadeResponse> entidadesPorTipoFiltrado(Long tipoEntidadeId, java.time.LocalDate de, java.time.LocalDate ate) {
    Long tenantId = requireTenant();
    java.time.Instant di = de == null ? null : de.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
    java.time.Instant fi = ate == null ? null : ate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).minusSeconds(1).toInstant();
    Map<Long, String> nomes = tipoEntidadeRepository.findAllByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged())
      .stream().collect(Collectors.toMap(e -> e.getId(), e -> e.getNome()));

    return entidadeRepository.countByTipoEntidadeFiltered(tenantId, tipoEntidadeId, di, fi).stream().map(row -> {
      Long id = (Long) row[0];
      long total = (Long) row[1];
      return new RelatorioEntidadeResponse(id, nomes.getOrDefault(id, ""), total);
    }).toList();
  }

  public List<RelatorioEntidadeComparativoResponse> entidadesComparativo(
      java.time.LocalDate de1, java.time.LocalDate ate1,
      java.time.LocalDate de2, java.time.LocalDate ate2) {
    Long tenantId = requireTenant();
    java.time.Instant d1 = de1 == null ? null : de1.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
    java.time.Instant f1 = ate1 == null ? null : ate1.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).minusSeconds(1).toInstant();
    java.time.Instant d2 = de2 == null ? null : de2.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
    java.time.Instant f2 = ate2 == null ? null : ate2.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).minusSeconds(1).toInstant();

    Map<Long, String> nomes = tipoEntidadeRepository.findAllByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged())
      .stream().collect(Collectors.toMap(e -> e.getId(), e -> e.getNome()));

    Map<Long, Long> p1 = entidadeRepository.countByTipoEntidadeFiltered(tenantId, null, d1, f1).stream()
      .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    Map<Long, Long> p2 = entidadeRepository.countByTipoEntidadeFiltered(tenantId, null, d2, f2).stream()
      .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

    return nomes.keySet().stream().map(id -> new RelatorioEntidadeComparativoResponse(
      id,
      nomes.getOrDefault(id, ""),
      p1.getOrDefault(id, 0L),
      p2.getOrDefault(id, 0L)
    )).toList();
  }

  public List<RelatorioContatoResponse> contatosPorTipo() {
    Long tenantId = requireTenant();
    return pessoaContatoRepository.countByTipo(tenantId).stream().map(row -> {
      String tipo = (String) row[0];
      long total = (Long) row[1];
      return new RelatorioContatoResponse(tipo, total);
    }).toList();
  }

  public RelatorioLocatarioStatusResponse locatariosStatus() {
    return new RelatorioLocatarioStatusResponse(
      locatarioRepository.countTotal(),
      locatarioRepository.countAtivos(),
      locatarioRepository.countBloqueados()
    );
  }

  public List<RelatorioPendenciaContatoResponse> pendenciasContato() {
    Long tenantId = requireTenant();
    List<Entidade> entidades = entidadeRepository.findAllAtivos(tenantId);
    if (entidades.isEmpty()) return List.of();

    List<ContatoTipo> obrigatorios = contatoTipoRepository.findAllByTenantId(tenantId).stream()
      .filter(ContatoTipo::isObrigatorio)
      .toList();
    if (obrigatorios.isEmpty()) return List.of();

    Set<String> codigosObrigatorios = obrigatorios.stream()
      .map(ContatoTipo::getCodigo)
      .collect(Collectors.toSet());

    Set<Long> pessoaIds = entidades.stream().map(Entidade::getPessoaId).collect(Collectors.toSet());
    Map<Long, Set<String>> contatosPorPessoa = pessoaContatoRepository
      .findAllByTenantIdAndPessoaIdIn(tenantId, pessoaIds)
      .stream()
      .collect(Collectors.groupingBy(
        c -> c.getPessoaId(),
        Collectors.mapping(c -> c.getTipo(), Collectors.toSet())
      ));

    Map<Long, String> pessoaNomes = pessoaRepository.findAllByTenantIdAndIdIn(tenantId, pessoaIds)
      .stream()
      .collect(Collectors.toMap(p -> p.getId(), p -> p.getNome()));

    return entidades.stream().flatMap(entidade -> {
      Set<String> existentes = contatosPorPessoa.getOrDefault(entidade.getPessoaId(), Set.of());
      return codigosObrigatorios.stream()
        .filter(codigo -> existentes.stream().noneMatch(e -> e.equalsIgnoreCase(codigo)))
        .map(codigo -> new RelatorioPendenciaContatoResponse(
          entidade.getId(),
          pessoaNomes.getOrDefault(entidade.getPessoaId(), ""),
          codigo
        ));
    }).toList();
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
