package com.ia.app.service;

import com.ia.app.domain.Empresa;
import com.ia.app.dto.EmpresaFilialRequest;
import com.ia.app.dto.EmpresaMatrizRequest;
import com.ia.app.dto.EmpresaUpdateRequest;
import com.ia.app.repository.EmpresaRepository;
import com.ia.app.repository.EmpresaSpecifications;
import com.ia.app.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class EmpresaService {

  private final EmpresaRepository repository;

  public EmpresaService(EmpresaRepository repository) {
    this.repository = repository;
  }

  public Page<Empresa> findAll(String nome, String cnpj, String tipo, Long matrizId, Boolean ativo, Pageable pageable) {
    Long tenantId = requireTenant();
    Specification<Empresa> spec = Specification
      .where(EmpresaSpecifications.tenantEquals(tenantId))
      .and(EmpresaSpecifications.nomeLike(nome))
      .and(EmpresaSpecifications.cnpjLike(cnpj))
      .and(EmpresaSpecifications.tipoEquals(tipo))
      .and(EmpresaSpecifications.matrizEquals(matrizId))
      .and(EmpresaSpecifications.ativoEquals(ativo));
    return repository.findAll(spec, pageable);
  }

  public Empresa getById(Long id) {
    Long tenantId = requireTenant();
    return repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("empresa_not_found"));
  }

  public Empresa createMatriz(EmpresaMatrizRequest request) {
    Long tenantId = requireTenant();
    validateCnpjUnique(tenantId, request.cnpj(), null);

    Empresa empresa = new Empresa();
    empresa.setTenantId(tenantId);
    empresa.setTipo(Empresa.TIPO_MATRIZ);
    empresa.setMatriz(null);
    empresa.setRazaoSocial(request.razaoSocial().trim());
    empresa.setNomeFantasia(clean(request.nomeFantasia()));
    empresa.setCnpj(request.cnpj().trim());
    empresa.setAtivo(request.ativo());
    return repository.save(empresa);
  }

  public Empresa createFilial(EmpresaFilialRequest request) {
    Long tenantId = requireTenant();
    validateCnpjUnique(tenantId, request.cnpj(), null);

    Empresa matriz = repository.findByIdAndTenantId(request.matrizId(), tenantId)
      .orElseThrow(() -> new EntityNotFoundException("empresa_matriz_not_found"));
    if (!Empresa.TIPO_MATRIZ.equals(matriz.getTipo())) {
      throw new IllegalArgumentException("empresa_matriz_invalida");
    }

    Empresa empresa = new Empresa();
    empresa.setTenantId(tenantId);
    empresa.setTipo(Empresa.TIPO_FILIAL);
    empresa.setMatriz(matriz);
    empresa.setRazaoSocial(request.razaoSocial().trim());
    empresa.setNomeFantasia(clean(request.nomeFantasia()));
    empresa.setCnpj(request.cnpj().trim());
    empresa.setAtivo(request.ativo());
    return repository.save(empresa);
  }

  public Empresa update(Long id, EmpresaUpdateRequest request) {
    Long tenantId = requireTenant();
    Empresa empresa = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("empresa_not_found"));
    validateCnpjUnique(tenantId, request.cnpj(), id);
    empresa.setRazaoSocial(request.razaoSocial().trim());
    empresa.setNomeFantasia(clean(request.nomeFantasia()));
    empresa.setCnpj(request.cnpj().trim());
    empresa.setAtivo(request.ativo());
    return repository.save(empresa);
  }

  public Empresa updateStatus(Long id, boolean ativo) {
    Empresa empresa = getById(id);
    empresa.setAtivo(ativo);
    return repository.save(empresa);
  }

  public void delete(Long id) {
    Long tenantId = requireTenant();
    Empresa empresa = repository.findByIdAndTenantId(id, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("empresa_not_found"));
    if (Empresa.TIPO_MATRIZ.equals(empresa.getTipo())
        && repository.existsByTenantIdAndMatrizId(tenantId, empresa.getId())) {
      throw new IllegalArgumentException("empresa_matriz_com_filiais");
    }
    repository.delete(empresa);
  }

  public List<Empresa> listFiliais(Long matrizId) {
    Long tenantId = requireTenant();
    Empresa matriz = repository.findByIdAndTenantId(matrizId, tenantId)
      .orElseThrow(() -> new EntityNotFoundException("empresa_matriz_not_found"));
    if (!Empresa.TIPO_MATRIZ.equals(matriz.getTipo())) {
      throw new IllegalArgumentException("empresa_matriz_invalida");
    }
    return repository.findAllByTenantIdAndMatrizIdOrderByRazaoSocialAsc(tenantId, matrizId);
  }

  private void validateCnpjUnique(Long tenantId, String cnpj, Long ignoredEmpresaId) {
    repository.findByTenantIdAndCnpj(tenantId, cnpj.trim()).ifPresent(existing -> {
      if (ignoredEmpresaId == null || !existing.getId().equals(ignoredEmpresaId)) {
        throw new IllegalArgumentException("empresa_cnpj_duplicado");
      }
    });
  }

  private String clean(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }

  private Long requireTenant() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      throw new IllegalStateException("tenant_required");
    }
    return tenantId;
  }
}
