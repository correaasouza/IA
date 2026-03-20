package com.ia.app.service;

import com.ia.app.dto.RhQualificacaoRequest;
import com.ia.app.dto.RhQualificacaoResponse;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RhQualificacaoService {

  private final JdbcTemplate jdbcTemplate;

  public RhQualificacaoService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional(readOnly = true)
  public List<RhQualificacaoResponse> list(String text, Boolean ativo) {
    Long tenantId = requireTenant();
    StringBuilder sql = new StringBuilder(
      "select id, nome, completo, tipo, ativo from rh_qualificacao where tenant_id = ?");
    var params = new java.util.ArrayList<Object>();
    params.add(tenantId);
    String normalizedText = normalizeOptionalText(text);
    if (normalizedText != null) {
      sql.append(" and lower(nome) like ?");
      params.add("%" + normalizedText.toLowerCase() + "%");
    }
    if (ativo != null) {
      sql.append(" and ativo = ?");
      params.add(ativo);
    }
    sql.append(" order by lower(nome), id");
    return jdbcTemplate.query(sql.toString(), this::mapRow, params.toArray());
  }

  @Transactional(readOnly = true)
  public RhQualificacaoResponse getById(Long id) {
    Long tenantId = requireTenant();
    return jdbcTemplate.query(
      "select id, nome, completo, tipo, ativo from rh_qualificacao where tenant_id = ? and id = ?",
      this::mapRow,
      tenantId, id).stream().findFirst().orElseThrow(() -> new EntityNotFoundException("rh_qualificacao_not_found"));
  }

  @Transactional
  public RhQualificacaoResponse create(RhQualificacaoRequest request) {
    Long tenantId = requireTenant();
    String nome = normalizeNome(request.nome());
    if (existsByName(tenantId, nome, null)) {
      throw new IllegalArgumentException("rh_qualificacao_nome_duplicado");
    }
    boolean ativo = request.ativo() == null || request.ativo();
    boolean completo = Boolean.TRUE.equals(request.completo());
    String tipo = normalizeTipo(request.tipo());
    Long id = jdbcTemplate.queryForObject(
      "insert into rh_qualificacao (tenant_id, nome, completo, tipo, ativo, created_at, updated_at) " +
        "values (?, ?, ?, ?, ?, now(), now()) returning id",
      Long.class,
      tenantId, nome, completo, tipo, ativo);
    return getById(id);
  }

  @Transactional
  public RhQualificacaoResponse update(Long id, RhQualificacaoRequest request) {
    Long tenantId = requireTenant();
    RhQualificacaoResponse current = getById(id);
    String nome = normalizeNome(request.nome());
    if (existsByName(tenantId, nome, id)) {
      throw new IllegalArgumentException("rh_qualificacao_nome_duplicado");
    }
    boolean ativo = request.ativo() == null ? current.ativo() : request.ativo();
    boolean completo = request.completo() == null ? current.completo() : request.completo();
    String tipo = normalizeTipo(request.tipo());
    int updated = jdbcTemplate.update(
      "update rh_qualificacao set nome = ?, completo = ?, tipo = ?, ativo = ?, updated_at = now() " +
        "where tenant_id = ? and id = ?",
      nome, completo, tipo, ativo, tenantId, id);
    if (updated == 0) throw new EntityNotFoundException("rh_qualificacao_not_found");
    return getById(id);
  }

  @Transactional
  public void delete(Long id) {
    Long tenantId = requireTenant();
    int updated = jdbcTemplate.update(
      "update rh_qualificacao set ativo = false, updated_at = now() where tenant_id = ? and id = ?",
      tenantId, id);
    if (updated == 0) throw new EntityNotFoundException("rh_qualificacao_not_found");
  }

  private boolean existsByName(Long tenantId, String nome, Long excludeId) {
    if (excludeId == null) {
      Boolean exists = jdbcTemplate.queryForObject(
        "select exists(select 1 from rh_qualificacao where tenant_id = ? and lower(nome) = lower(?))",
        Boolean.class,
        tenantId, nome);
      return Boolean.TRUE.equals(exists);
    }
    Boolean exists = jdbcTemplate.queryForObject(
      "select exists(select 1 from rh_qualificacao where tenant_id = ? and lower(nome) = lower(?) and id <> ?)",
      Boolean.class,
      tenantId, nome, excludeId);
    return Boolean.TRUE.equals(exists);
  }

  private RhQualificacaoResponse mapRow(ResultSet rs, int rowNum) throws SQLException {
    return new RhQualificacaoResponse(
      rs.getLong("id"),
      rs.getString("nome"),
      rs.getBoolean("completo"),
      rs.getString("tipo"),
      rs.getBoolean("ativo"));
  }

  private String normalizeNome(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("rh_qualificacao_nome_required");
    }
    String normalized = value.trim();
    if (normalized.length() > 120) {
      throw new IllegalArgumentException("rh_qualificacao_nome_too_long");
    }
    return normalized;
  }

  private String normalizeTipo(String value) {
    if (value == null || value.isBlank()) return null;
    String normalized = value.trim().toUpperCase();
    if (normalized.length() > 1) {
      throw new IllegalArgumentException("rh_qualificacao_tipo_invalid");
    }
    return normalized;
  }

  private String normalizeOptionalText(String value) {
    if (value == null || value.isBlank()) return null;
    return value.trim();
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) throw new IllegalStateException("tenant_required");
    return tenantId;
  }
}
