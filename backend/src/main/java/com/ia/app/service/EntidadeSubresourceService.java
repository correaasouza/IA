package com.ia.app.service;

import com.ia.app.domain.EntidadeContato;
import com.ia.app.domain.EntidadeContatoForma;
import com.ia.app.domain.EntidadeDocumentacao;
import com.ia.app.domain.EntidadeEndereco;
import com.ia.app.domain.EntidadeFamiliar;
import com.ia.app.domain.Pessoa;
import com.ia.app.domain.RegistroEntidade;
import com.ia.app.dto.EntidadeContatoFormaRequest;
import com.ia.app.dto.EntidadeContatoFormaResponse;
import com.ia.app.dto.EntidadeContatoRequest;
import com.ia.app.dto.EntidadeContatoResponse;
import com.ia.app.dto.EntidadeDocumentacaoRequest;
import com.ia.app.dto.EntidadeDocumentacaoResponse;
import com.ia.app.dto.EntidadeEnderecoRequest;
import com.ia.app.dto.EntidadeEnderecoResponse;
import com.ia.app.dto.EntidadeFamiliarRequest;
import com.ia.app.dto.EntidadeFamiliarResponse;
import com.ia.app.repository.EntidadeContatoFormaRepository;
import com.ia.app.repository.EntidadeContatoRepository;
import com.ia.app.repository.EntidadeDocumentacaoRepository;
import com.ia.app.repository.EntidadeEnderecoRepository;
import com.ia.app.repository.EntidadeFamiliarRepository;
import com.ia.app.repository.PessoaRepository;
import com.ia.app.repository.RegistroEntidadeRepository;
import com.ia.app.util.CpfCnpjValidator;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntidadeSubresourceService {

  private static final Set<String> TIPOS_REGISTRO = Set.of("CPF", "CNPJ", "ID_ESTRANGEIRO");
  private static final Set<String> TIPOS_ENDERECO = Set.of(
    "RESIDENCIAL", "COMERCIAL", "ENTREGA", "COBRANCA", "OUTRO", "CORRESPONDENCIA");
  private static final Set<String> TIPOS_CONTATO = Set.of(
    "EMAIL", "FONE_CELULAR", "FONE_RESIDENCIAL", "FONE_COMERCIAL", "FACEBOOK", "WHATSAPP");
  private static final Set<String> TIPOS_PARENTESCO = Set.of(
    "PAI", "FILHO", "IRMAO", "IRMA", "TIO", "TIA", "PRIMO", "PRIMA", "VO", "VOMAE", "BISAVO", "BISAVOMAE", "OUTROS");
  private static final Pattern EMAIL_REGEX = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
  private static final Pattern PHONE_REGEX = Pattern.compile("^(\\(?\\d{2}\\)?\\s?)?\\d{4,5}-?\\d{4}$");

  private final RegistroEntidadeContextoService contextoService;
  private final RegistroEntidadeRepository registroRepository;
  private final PessoaRepository pessoaRepository;
  private final EntidadeDocumentacaoRepository documentacaoRepository;
  private final EntidadeEnderecoRepository enderecoRepository;
  private final EntidadeContatoRepository contatoRepository;
  private final EntidadeContatoFormaRepository contatoFormaRepository;
  private final EntidadeFamiliarRepository familiarRepository;

  public EntidadeSubresourceService(
      RegistroEntidadeContextoService contextoService,
      RegistroEntidadeRepository registroRepository,
      PessoaRepository pessoaRepository,
      EntidadeDocumentacaoRepository documentacaoRepository,
      EntidadeEnderecoRepository enderecoRepository,
      EntidadeContatoRepository contatoRepository,
      EntidadeContatoFormaRepository contatoFormaRepository,
      EntidadeFamiliarRepository familiarRepository) {
    this.contextoService = contextoService;
    this.registroRepository = registroRepository;
    this.pessoaRepository = pessoaRepository;
    this.documentacaoRepository = documentacaoRepository;
    this.enderecoRepository = enderecoRepository;
    this.contatoRepository = contatoRepository;
    this.contatoFormaRepository = contatoFormaRepository;
    this.familiarRepository = familiarRepository;
  }

  @Transactional(readOnly = true)
  public EntidadeDocumentacaoResponse getDocumentacao(Long tipoEntidadeId, Long entidadeId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    EntidadeDocumentacao doc = documentacaoRepository
      .findByTenantIdAndEmpresaIdAndRegistroEntidadeId(scope.tenantId(), scope.empresaId(), registro.getId())
      .orElseThrow(() -> new EntityNotFoundException("entidade_documentacao_not_found"));
    return toDocumentacaoResponse(doc);
  }

  @Transactional
  public EntidadeDocumentacaoResponse upsertDocumentacao(
      Long tipoEntidadeId,
      Long entidadeId,
      EntidadeDocumentacaoRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    EntidadeDocumentacao entity = documentacaoRepository
      .findByTenantIdAndEmpresaIdAndRegistroEntidadeId(scope.tenantId(), scope.empresaId(), registro.getId())
      .orElseGet(EntidadeDocumentacao::new);

    entity.setTenantId(scope.tenantId());
    entity.setEmpresaId(scope.empresaId());
    entity.setRegistroEntidadeId(registro.getId());

    String tipoRegistro = normalizeTipoRegistro(request.tipoRegistroFederal());
    String registroFederal = normalizeDocumento(tipoRegistro, request.registroFederal());
    entity.setTipoRegistroFederal(tipoRegistro);
    entity.setRegistroFederal(registroFederal);
    entity.setRegistroFederalNormalizado(normalizeDocumentoBusca(tipoRegistro, registroFederal));
    entity.setRegistroFederalHash(hashSha256(entity.getRegistroFederalNormalizado()));

    entity.setRegistroFederalDataEmissao(request.registroFederalDataEmissao());
    entity.setRg(trim(request.rg(), 30));
    entity.setRgTipo(trim(request.rgTipo(), 30));
    entity.setRgDataEmissao(request.rgDataEmissao());
    entity.setRgUfEmissao(normalizeUf(request.rgUfEmissao()));
    entity.setRegistroEstadual(trim(request.registroEstadual(), 40));
    entity.setRegistroEstadualDataEmissao(request.registroEstadualDataEmissao());
    entity.setRegistroEstadualUf(normalizeUf(request.registroEstadualUf()));
    entity.setRegistroEstadualContribuinte(Boolean.TRUE.equals(request.registroEstadualContribuinte()));
    entity.setRegistroEstadualConsumidorFinal(Boolean.TRUE.equals(request.registroEstadualConsumidorFinal()));
    entity.setRegistroMunicipal(trim(request.registroMunicipal(), 40));
    entity.setRegistroMunicipalDataEmissao(request.registroMunicipalDataEmissao());
    entity.setCnh(trim(request.cnh(), 20));
    entity.setCnhCategoria(trim(request.cnhCategoria(), 5));
    entity.setCnhObservacao(trim(request.cnhObservacao(), 255));
    entity.setCnhDataEmissao(request.cnhDataEmissao());
    entity.setSuframa(trim(request.suframa(), 30));
    entity.setRntc(trim(request.rntc(), 30));
    entity.setPis(trim(request.pis(), 20));
    entity.setTituloEleitor(trim(request.tituloEleitor(), 20));
    entity.setTituloEleitorZona(trim(request.tituloEleitorZona(), 10));
    entity.setTituloEleitorSecao(trim(request.tituloEleitorSecao(), 10));
    entity.setCtps(trim(request.ctps(), 20));
    entity.setCtpsSerie(trim(request.ctpsSerie(), 20));
    entity.setCtpsDataEmissao(request.ctpsDataEmissao());
    entity.setCtpsUfEmissao(normalizeUf(request.ctpsUfEmissao()));
    entity.setMilitarNumero(trim(request.militarNumero(), 40));
    entity.setMilitarSerie(trim(request.militarSerie(), 40));
    entity.setMilitarCategoria(trim(request.militarCategoria(), 20));
    entity.setNumeroNif(trim(request.numeroNif(), 40));
    entity.setMotivoNaoNif(normalizeMotivoNif(request.motivoNaoNif()));

    EntidadeDocumentacao saved = documentacaoRepository.save(entity);
    return toDocumentacaoResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<EntidadeEnderecoResponse> listEnderecos(Long tipoEntidadeId, Long entidadeId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    return enderecoRepository
      .findAllByTenantIdAndEmpresaIdAndRegistroEntidadeIdOrderByIdAsc(scope.tenantId(), scope.empresaId(), registro.getId())
      .stream()
      .map(this::toEnderecoResponse)
      .toList();
  }

  @Transactional
  public EntidadeEnderecoResponse createEndereco(Long tipoEntidadeId, Long entidadeId, EntidadeEnderecoRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    EntidadeEndereco entity = new EntidadeEndereco();
    entity.setTenantId(scope.tenantId());
    entity.setEmpresaId(scope.empresaId());
    entity.setRegistroEntidadeId(registro.getId());
    applyEndereco(entity, request);
    return toEnderecoResponse(enderecoRepository.save(entity));
  }

  @Transactional
  public EntidadeEnderecoResponse updateEndereco(
      Long tipoEntidadeId,
      Long entidadeId,
      Long enderecoId,
      EntidadeEnderecoRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    EntidadeEndereco entity = enderecoRepository
      .findByIdAndTenantIdAndEmpresaIdAndRegistroEntidadeId(enderecoId, scope.tenantId(), scope.empresaId(), registro.getId())
      .orElseThrow(() -> new EntityNotFoundException("entidade_endereco_not_found"));
    applyEndereco(entity, request);
    return toEnderecoResponse(enderecoRepository.save(entity));
  }

  @Transactional
  public void deleteEndereco(Long tipoEntidadeId, Long entidadeId, Long enderecoId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    EntidadeEndereco entity = enderecoRepository
      .findByIdAndTenantIdAndEmpresaIdAndRegistroEntidadeId(enderecoId, scope.tenantId(), scope.empresaId(), registro.getId())
      .orElseThrow(() -> new EntityNotFoundException("entidade_endereco_not_found"));
    enderecoRepository.delete(entity);
  }

  @Transactional(readOnly = true)
  public List<EntidadeContatoResponse> listContatos(Long tipoEntidadeId, Long entidadeId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    return contatoRepository
      .findAllByTenantIdAndEmpresaIdAndRegistroEntidadeIdOrderByIdAsc(scope.tenantId(), scope.empresaId(), registro.getId())
      .stream()
      .map(this::toContatoResponse)
      .toList();
  }

  @Transactional
  public EntidadeContatoResponse createContato(Long tipoEntidadeId, Long entidadeId, EntidadeContatoRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    EntidadeContato entity = new EntidadeContato();
    entity.setTenantId(scope.tenantId());
    entity.setEmpresaId(scope.empresaId());
    entity.setRegistroEntidadeId(registro.getId());
    applyContato(entity, request);
    return toContatoResponse(contatoRepository.save(entity));
  }

  @Transactional
  public EntidadeContatoResponse updateContato(
      Long tipoEntidadeId,
      Long entidadeId,
      Long contatoId,
      EntidadeContatoRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    EntidadeContato entity = getContato(scope, registro.getId(), contatoId);
    applyContato(entity, request);
    return toContatoResponse(contatoRepository.save(entity));
  }

  @Transactional
  public void deleteContato(Long tipoEntidadeId, Long entidadeId, Long contatoId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    EntidadeContato entity = getContato(scope, registro.getId(), contatoId);
    contatoRepository.delete(entity);
  }

  @Transactional(readOnly = true)
  public List<EntidadeContatoFormaResponse> listContatoFormas(Long tipoEntidadeId, Long entidadeId, Long contatoId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    getContato(scope, registro.getId(), contatoId);
    return contatoFormaRepository
      .findAllByTenantIdAndEmpresaIdAndRegistroEntidadeIdAndContatoIdOrderByIdAsc(
        scope.tenantId(), scope.empresaId(), registro.getId(), contatoId)
      .stream()
      .map(this::toContatoFormaResponse)
      .toList();
  }

  @Transactional
  public EntidadeContatoFormaResponse createContatoForma(
      Long tipoEntidadeId,
      Long entidadeId,
      Long contatoId,
      EntidadeContatoFormaRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    getContato(scope, registro.getId(), contatoId);
    EntidadeContatoForma entity = new EntidadeContatoForma();
    entity.setTenantId(scope.tenantId());
    entity.setEmpresaId(scope.empresaId());
    entity.setRegistroEntidadeId(registro.getId());
    entity.setContatoId(contatoId);
    applyContatoForma(entity, request);
    return toContatoFormaResponse(contatoFormaRepository.save(entity));
  }

  @Transactional
  public EntidadeContatoFormaResponse updateContatoForma(
      Long tipoEntidadeId,
      Long entidadeId,
      Long contatoId,
      Long formaId,
      EntidadeContatoFormaRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    getContato(scope, registro.getId(), contatoId);
    EntidadeContatoForma entity = contatoFormaRepository
      .findByIdAndTenantIdAndEmpresaIdAndRegistroEntidadeIdAndContatoId(
        formaId, scope.tenantId(), scope.empresaId(), registro.getId(), contatoId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_contato_forma_not_found"));
    applyContatoForma(entity, request);
    return toContatoFormaResponse(contatoFormaRepository.save(entity));
  }

  @Transactional
  public void deleteContatoForma(
      Long tipoEntidadeId,
      Long entidadeId,
      Long contatoId,
      Long formaId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    getContato(scope, registro.getId(), contatoId);
    EntidadeContatoForma entity = contatoFormaRepository
      .findByIdAndTenantIdAndEmpresaIdAndRegistroEntidadeIdAndContatoId(
        formaId, scope.tenantId(), scope.empresaId(), registro.getId(), contatoId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_contato_forma_not_found"));
    contatoFormaRepository.delete(entity);
  }

  @Transactional(readOnly = true)
  public List<EntidadeFamiliarResponse> listFamiliares(Long tipoEntidadeId, Long entidadeId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    return familiarRepository
      .findAllByTenantIdAndEmpresaIdAndRegistroEntidadeIdOrderByIdAsc(scope.tenantId(), scope.empresaId(), registro.getId())
      .stream()
      .map(item -> toFamiliarResponse(scope.tenantId(), item))
      .toList();
  }

  @Transactional
  public EntidadeFamiliarResponse createFamiliar(Long tipoEntidadeId, Long entidadeId, EntidadeFamiliarRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    EntidadeFamiliar entity = new EntidadeFamiliar();
    entity.setTenantId(scope.tenantId());
    entity.setEmpresaId(scope.empresaId());
    entity.setRegistroEntidadeId(registro.getId());
    applyFamiliar(scope, registro.getId(), entity, request);
    return toFamiliarResponse(scope.tenantId(), familiarRepository.save(entity));
  }

  @Transactional
  public EntidadeFamiliarResponse updateFamiliar(
      Long tipoEntidadeId,
      Long entidadeId,
      Long familiarId,
      EntidadeFamiliarRequest request) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    EntidadeFamiliar entity = familiarRepository
      .findByIdAndTenantIdAndEmpresaIdAndRegistroEntidadeId(familiarId, scope.tenantId(), scope.empresaId(), registro.getId())
      .orElseThrow(() -> new EntityNotFoundException("entidade_familiar_not_found"));
    applyFamiliar(scope, registro.getId(), entity, request);
    return toFamiliarResponse(scope.tenantId(), familiarRepository.save(entity));
  }

  @Transactional
  public void deleteFamiliar(Long tipoEntidadeId, Long entidadeId, Long familiarId) {
    var scope = contextoService.resolveObrigatorio(tipoEntidadeId);
    RegistroEntidade registro = getRegistro(scope, entidadeId);
    EntidadeFamiliar entity = familiarRepository
      .findByIdAndTenantIdAndEmpresaIdAndRegistroEntidadeId(familiarId, scope.tenantId(), scope.empresaId(), registro.getId())
      .orElseThrow(() -> new EntityNotFoundException("entidade_familiar_not_found"));
    familiarRepository.delete(entity);
  }

  private RegistroEntidade getRegistro(RegistroEntidadeContextoService.RegistroEntidadeScope scope, Long entidadeId) {
    RegistroEntidade registro = registroRepository
      .findByIdAndTenantIdAndTipoEntidadeConfigAgrupadorId(entidadeId, scope.tenantId(), scope.tipoEntidadeConfigAgrupadorId())
      .orElseThrow(() -> new EntityNotFoundException("registro_entidade_not_found"));
    if (!scope.empresaId().equals(registro.getEmpresaId())) {
      throw new EntityNotFoundException("registro_entidade_not_found");
    }
    return registro;
  }

  private EntidadeContato getContato(RegistroEntidadeContextoService.RegistroEntidadeScope scope, Long registroId, Long contatoId) {
    return contatoRepository
      .findByIdAndTenantIdAndEmpresaIdAndRegistroEntidadeId(contatoId, scope.tenantId(), scope.empresaId(), registroId)
      .orElseThrow(() -> new EntityNotFoundException("entidade_contato_not_found"));
  }

  private EntidadeDocumentacaoResponse toDocumentacaoResponse(EntidadeDocumentacao d) {
    return new EntidadeDocumentacaoResponse(
      d.getId(), d.getRegistroEntidadeId(), d.getTipoRegistroFederal(), d.getRegistroFederal(),
      d.getRegistroFederalNormalizado(), d.getRegistroFederalHash(), d.getRegistroFederalDataEmissao(),
      d.getRg(), d.getRgTipo(), d.getRgDataEmissao(), d.getRgUfEmissao(),
      d.getRegistroEstadual(), d.getRegistroEstadualDataEmissao(), d.getRegistroEstadualUf(),
      d.isRegistroEstadualContribuinte(), d.isRegistroEstadualConsumidorFinal(),
      d.getRegistroMunicipal(), d.getRegistroMunicipalDataEmissao(), d.getCnh(), d.getCnhCategoria(),
      d.getCnhObservacao(), d.getCnhDataEmissao(), d.getSuframa(), d.getRntc(), d.getPis(),
      d.getTituloEleitor(), d.getTituloEleitorZona(), d.getTituloEleitorSecao(),
      d.getCtps(), d.getCtpsSerie(), d.getCtpsDataEmissao(), d.getCtpsUfEmissao(),
      d.getMilitarNumero(), d.getMilitarSerie(), d.getMilitarCategoria(), d.getNumeroNif(),
      d.getMotivoNaoNif(), d.getVersion());
  }

  private EntidadeEnderecoResponse toEnderecoResponse(EntidadeEndereco e) {
    return new EntidadeEnderecoResponse(
      e.getId(), e.getRegistroEntidadeId(), e.getNome(), e.getCep(), e.getCepEstrangeiro(), e.getPais(),
      e.getPaisCodigoIbge(), e.getUf(), e.getUfCodigoIbge(), e.getMunicipio(), e.getMunicipioCodigoIbge(),
      e.getLogradouro(), e.getLogradouroTipo(), e.getNumero(), e.getComplemento(), e.getEnderecoTipo(),
      e.isPrincipal(), e.getLongitude(), e.getLatitude(), e.getEstadoProvinciaRegiaoEstrangeiro(), e.getVersion());
  }

  private EntidadeContatoResponse toContatoResponse(EntidadeContato c) {
    return new EntidadeContatoResponse(c.getId(), c.getRegistroEntidadeId(), c.getNome(), c.getCargo(), c.getVersion());
  }

  private EntidadeContatoFormaResponse toContatoFormaResponse(EntidadeContatoForma f) {
    return new EntidadeContatoFormaResponse(
      f.getId(), f.getContatoId(), f.getTipoContato(), f.getValor(), f.getValorNormalizado(), f.isPreferencial(), f.getVersion());
  }

  private EntidadeFamiliarResponse toFamiliarResponse(Long tenantId, EntidadeFamiliar f) {
    String nomeParente = registroRepository.findById(f.getEntidadeParenteId())
      .flatMap(reg -> pessoaRepository.findByIdAndTenantId(reg.getPessoaId(), tenantId))
      .map(Pessoa::getNome)
      .orElse(null);
    return new EntidadeFamiliarResponse(
      f.getId(), f.getRegistroEntidadeId(), f.getEntidadeParenteId(), nomeParente, f.isDependente(), f.getParentesco(), f.getVersion());
  }

  private void applyEndereco(EntidadeEndereco entity, EntidadeEnderecoRequest request) {
    String tipo = normalizeTipoEndereco(request.enderecoTipo());
    entity.setNome(trim(request.nome(), 120));
    entity.setCep(normalizeCep(request.cep()));
    entity.setCepEstrangeiro(trim(request.cepEstrangeiro(), 20));
    entity.setPais(trim(request.pais(), 80));
    entity.setPaisCodigoIbge(request.paisCodigoIbge());
    entity.setUf(normalizeUf(request.uf()));
    entity.setUfCodigoIbge(trim(request.ufCodigoIbge(), 10));
    entity.setMunicipio(trim(request.municipio(), 120));
    entity.setMunicipioCodigoIbge(trim(request.municipioCodigoIbge(), 10));
    entity.setLogradouro(trim(request.logradouro(), 200));
    entity.setLogradouroTipo(trim(request.logradouroTipo(), 40));
    entity.setNumero(trim(request.numero(), 20));
    entity.setComplemento(trim(request.complemento(), 120));
    entity.setEnderecoTipo(tipo);
    entity.setPrincipal(Boolean.TRUE.equals(request.principal()));
    entity.setLongitude(request.longitude());
    entity.setLatitude(request.latitude());
    entity.setEstadoProvinciaRegiaoEstrangeiro(trim(request.estadoProvinciaRegiaoEstrangeiro(), 60));
  }

  private void applyContato(EntidadeContato entity, EntidadeContatoRequest request) {
    entity.setNome(trim(request.nome(), 120));
    entity.setCargo(trim(request.cargo(), 120));
  }

  private void applyContatoForma(EntidadeContatoForma entity, EntidadeContatoFormaRequest request) {
    String tipo = normalizeTipoContato(request.tipoContato());
    String valor = normalizeContatoValor(tipo, request.valor());
    entity.setTipoContato(tipo);
    entity.setValor(valor);
    entity.setValorNormalizado(normalizeContatoValorBusca(tipo, valor));
    entity.setPreferencial(Boolean.TRUE.equals(request.preferencial()));
  }

  private void applyFamiliar(
      RegistroEntidadeContextoService.RegistroEntidadeScope scope,
      Long registroEntidadeId,
      EntidadeFamiliar entity,
      EntidadeFamiliarRequest request) {
    if (request.entidadeParenteId() == null || request.entidadeParenteId() <= 0) {
      throw new IllegalArgumentException("entidade_familiar_parente_required");
    }
    if (request.entidadeParenteId().equals(registroEntidadeId)) {
      throw new IllegalArgumentException("entidade_familiar_parente_self");
    }
    RegistroEntidade parente = getRegistro(scope, request.entidadeParenteId());
    entity.setEntidadeParenteId(parente.getId());
    entity.setDependente(Boolean.TRUE.equals(request.dependente()));
    entity.setParentesco(normalizeParentesco(request.parentesco()));
  }

  private String normalizeTipoRegistro(String value) {
    String normalized = trim(value, 20);
    if (normalized == null) throw new IllegalArgumentException("entidade_documentacao_tipo_registro_required");
    normalized = normalized.toUpperCase();
    if (!TIPOS_REGISTRO.contains(normalized)) throw new IllegalArgumentException("entidade_documentacao_tipo_registro_invalid");
    return normalized;
  }

  private String normalizeDocumento(String tipoRegistro, String value) {
    String normalized = trim(value, 40);
    if (normalized == null) throw new IllegalArgumentException("entidade_documentacao_registro_federal_required");
    if ("CPF".equals(tipoRegistro) || "CNPJ".equals(tipoRegistro)) {
      String digits = normalized.replaceAll("\\D", "");
      if (!CpfCnpjValidator.isValid(digits)) throw new IllegalArgumentException("entidade_documentacao_registro_federal_invalid");
      return digits;
    }
    return normalized.toUpperCase();
  }

  private String normalizeDocumentoBusca(String tipoRegistro, String value) {
    if ("CPF".equals(tipoRegistro) || "CNPJ".equals(tipoRegistro)) return value.replaceAll("\\D", "");
    return value.toUpperCase();
  }

  private String normalizeTipoEndereco(String value) {
    String normalized = trim(value, 20);
    if (normalized == null) throw new IllegalArgumentException("entidade_endereco_tipo_required");
    normalized = normalized.toUpperCase();
    if (!TIPOS_ENDERECO.contains(normalized)) throw new IllegalArgumentException("entidade_endereco_tipo_invalid");
    return normalized;
  }

  private String normalizeTipoContato(String value) {
    String normalized = trim(value, 30);
    if (normalized == null) throw new IllegalArgumentException("entidade_contato_forma_tipo_required");
    normalized = normalized.toUpperCase();
    if (!TIPOS_CONTATO.contains(normalized)) throw new IllegalArgumentException("entidade_contato_forma_tipo_invalid");
    return normalized;
  }

  private String normalizeParentesco(String value) {
    String normalized = trim(value, 20);
    if (normalized == null) throw new IllegalArgumentException("entidade_familiar_parentesco_required");
    normalized = normalized.toUpperCase();
    if (!TIPOS_PARENTESCO.contains(normalized)) throw new IllegalArgumentException("entidade_familiar_parentesco_invalid");
    return normalized;
  }

  private String normalizeContatoValor(String tipo, String value) {
    String normalized = trim(value, 200);
    if (normalized == null) throw new IllegalArgumentException("entidade_contato_forma_valor_required");
    if ("EMAIL".equals(tipo) && !EMAIL_REGEX.matcher(normalized).matches()) {
      throw new IllegalArgumentException("entidade_contato_forma_email_invalid");
    }
    if (("FONE_CELULAR".equals(tipo) || "FONE_RESIDENCIAL".equals(tipo) || "FONE_COMERCIAL".equals(tipo) || "WHATSAPP".equals(tipo))
      && !PHONE_REGEX.matcher(normalized).matches()) {
      throw new IllegalArgumentException("entidade_contato_forma_telefone_invalid");
    }
    return normalized;
  }

  private String normalizeContatoValorBusca(String tipo, String value) {
    if ("EMAIL".equals(tipo)) return value.toLowerCase();
    if ("FONE_CELULAR".equals(tipo) || "FONE_RESIDENCIAL".equals(tipo) || "FONE_COMERCIAL".equals(tipo) || "WHATSAPP".equals(tipo)) {
      return value.replaceAll("\\D", "");
    }
    return value.toUpperCase();
  }

  private String normalizeCep(String value) {
    String normalized = trim(value, 9);
    if (normalized == null) return null;
    String digits = normalized.replaceAll("\\D", "");
    if (digits.isEmpty()) return null;
    if (digits.length() != 8) throw new IllegalArgumentException("entidade_endereco_cep_invalid");
    return digits;
  }

  private String normalizeUf(String value) {
    String normalized = trim(value, 2);
    if (normalized == null) return null;
    normalized = normalized.toUpperCase();
    if (!normalized.matches("[A-Z]{2}")) throw new IllegalArgumentException("entidade_endereco_uf_invalid");
    return normalized;
  }

  private Short normalizeMotivoNif(Short value) {
    if (value == null) return null;
    if (value < 0 || value > 2) throw new IllegalArgumentException("entidade_documentacao_motivo_nif_invalid");
    return value;
  }

  private String trim(String value, int maxLength) {
    if (value == null) return null;
    String normalized = value.trim();
    if (normalized.isEmpty()) return null;
    if (normalized.length() > maxLength) throw new IllegalArgumentException("entidade_field_too_long");
    return normalized;
  }

  private String hashSha256(String value) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("sha256_not_supported");
    }
  }
}
