package com.ia.app.service;

import com.ia.app.domain.ContatoTipoPorEntidade;
import com.ia.app.dto.RelatorioContatoResponse;
import com.ia.app.dto.RelatorioEntidadeResponse;
import com.ia.app.dto.RelatorioEntidadeComparativoResponse;
import com.ia.app.dto.RelatorioLocatarioStatusResponse;
import com.ia.app.dto.RelatorioPendenciaContatoResponse;
import com.ia.app.repository.ContatoRepository;
import com.ia.app.repository.ContatoTipoPorEntidadeRepository;
import com.ia.app.repository.ContatoTipoRepository;
import com.ia.app.repository.EntidadeDefinicaoRepository;
import com.ia.app.repository.EntidadeRegistroRepository;
import com.ia.app.repository.LocatarioRepository;
import com.ia.app.tenant.TenantContext;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RelatorioService {

  private final EntidadeRegistroRepository entidadeRegistroRepository;
  private final EntidadeDefinicaoRepository entidadeDefinicaoRepository;
  private final ContatoRepository contatoRepository;
  private final LocatarioRepository locatarioRepository;
  private final ContatoTipoPorEntidadeRepository contatoTipoPorEntidadeRepository;
  private final ContatoTipoRepository contatoTipoRepository;

  public RelatorioService(EntidadeRegistroRepository entidadeRegistroRepository,
      EntidadeDefinicaoRepository entidadeDefinicaoRepository,
      ContatoRepository contatoRepository,
      LocatarioRepository locatarioRepository,
      ContatoTipoPorEntidadeRepository contatoTipoPorEntidadeRepository,
      ContatoTipoRepository contatoTipoRepository) {
    this.entidadeRegistroRepository = entidadeRegistroRepository;
    this.entidadeDefinicaoRepository = entidadeDefinicaoRepository;
    this.contatoRepository = contatoRepository;
    this.locatarioRepository = locatarioRepository;
    this.contatoTipoPorEntidadeRepository = contatoTipoPorEntidadeRepository;
    this.contatoTipoRepository = contatoTipoRepository;
  }

  public List<RelatorioEntidadeResponse> entidadesPorTipo() {
    Long tenantId = requireTenant();
    Map<Long, String> nomes = entidadeDefinicaoRepository.findAllByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged())
      .stream().collect(Collectors.toMap(e -> e.getId(), e -> e.getNome()));

    return entidadeRegistroRepository.countByEntidade(tenantId).stream().map(row -> {
      Long id = (Long) row[0];
      long total = (Long) row[1];
      return new RelatorioEntidadeResponse(id, nomes.getOrDefault(id, ""), total);
    }).toList();
  }

  public List<RelatorioEntidadeResponse> entidadesPorTipoFiltrado(Long entidadeId, java.time.LocalDate de, java.time.LocalDate ate) {
    Long tenantId = requireTenant();
    java.time.Instant di = de == null ? null : de.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
    java.time.Instant fi = ate == null ? null : ate.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).minusSeconds(1).toInstant();
    Map<Long, String> nomes = entidadeDefinicaoRepository.findAllByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged())
      .stream().collect(Collectors.toMap(e -> e.getId(), e -> e.getNome()));

    return entidadeRegistroRepository.countByEntidadeFiltered(tenantId, entidadeId, di, fi).stream().map(row -> {
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

    Map<Long, String> nomes = entidadeDefinicaoRepository.findAllByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged())
      .stream().collect(Collectors.toMap(e -> e.getId(), e -> e.getNome()));

    Map<Long, Long> p1 = entidadeRegistroRepository.countByEntidadeFiltered(tenantId, null, d1, f1).stream()
      .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    Map<Long, Long> p2 = entidadeRegistroRepository.countByEntidadeFiltered(tenantId, null, d2, f2).stream()
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
    return contatoRepository.countByTipo(tenantId).stream().map(row -> {
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
    var entidades = entidadeRegistroRepository.findAllAtivos(tenantId);
    var tiposPorEntidade = contatoTipoPorEntidadeRepository.findAllByTenantId(tenantId);
    var tipos = contatoTipoRepository.findAllByTenantId(tenantId);
    Map<Long, String> tipoIdToCodigo = tipos.stream().collect(Collectors.toMap(t -> t.getId(), t -> t.getCodigo()));

    return entidades.stream().flatMap(entidade -> {
      var obrigatorios = tiposPorEntidade.stream()
        .filter(t -> t.getEntidadeDefinicaoId().equals(entidade.getEntidadeDefinicaoId()))
        .filter(ContatoTipoPorEntidade::isObrigatorio)
        .toList();
      if (obrigatorios.isEmpty()) return java.util.stream.Stream.<RelatorioPendenciaContatoResponse>empty();
      var contatos = contatoRepository.findAllByTenantIdAndEntidadeRegistroId(tenantId, entidade.getId());
      return obrigatorios.stream().filter(t -> {
        String codigo = tipoIdToCodigo.getOrDefault(t.getContatoTipoId(), "");
        return contatos.stream().noneMatch(c -> c.getTipo().equalsIgnoreCase(codigo));
      }).map(t -> new RelatorioPendenciaContatoResponse(
        entidade.getId(),
        entidade.getNome(),
        tipoIdToCodigo.getOrDefault(t.getContatoTipoId(), "")
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
