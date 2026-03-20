package com.ia.app.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ia.app.domain.CepCache;
import com.ia.app.dto.CepLookupResponse;
import com.ia.app.repository.CepCacheRepository;
import jakarta.persistence.EntityNotFoundException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CepLookupService {

  private final CepCacheRepository repository;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public CepLookupService(CepCacheRepository repository, ObjectMapper objectMapper) {
    this.repository = repository;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(4))
      .build();
  }

  @Transactional
  public CepLookupResponse lookup(String rawCep) {
    String cep = normalizeCep(rawCep);
    Optional<CepCache> cached = repository.findByCep(cep);
    if (cached.isPresent()) {
      CepCache row = cached.get();
      String ufCodigoIbge = resolveUfCodigoIbge(row.getIbge());
      return new CepLookupResponse(
        row.getCep(),
        row.getLogradouro(),
        row.getBairro(),
        row.getLocalidade(),
        row.getUf(),
        row.getIbge(),
        ufCodigoIbge,
        "CACHE");
    }

    ViaCepResponse external = fetchViaCep(cep);
    if (external == null || Boolean.TRUE.equals(external.erro())) {
      throw new EntityNotFoundException("cep_not_found");
    }

    CepCache entity = new CepCache();
    entity.setCep(cep);
    entity.setLogradouro(trim(external.logradouro(), 200));
    entity.setBairro(trim(external.bairro(), 120));
    entity.setLocalidade(trim(external.localidade(), 120));
    entity.setUf(normalizeUf(external.uf()));
    entity.setIbge(trim(external.ibge(), 10));
    entity.setOrigem("EXTERNAL");
    CepCache saved = repository.save(entity);
    String ufCodigoIbge = resolveUfCodigoIbge(saved.getIbge());
    return new CepLookupResponse(
      saved.getCep(),
      saved.getLogradouro(),
      saved.getBairro(),
      saved.getLocalidade(),
      saved.getUf(),
      saved.getIbge(),
      ufCodigoIbge,
      "EXTERNAL");
  }

  private String resolveUfCodigoIbge(String ibgeCidade) {
    String digits = (ibgeCidade == null ? "" : ibgeCidade).replaceAll("\\D", "");
    if (digits.length() < 2) return null;
    return digits.substring(0, 2);
  }

  private ViaCepResponse fetchViaCep(String cep) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://viacep.com.br/ws/" + cep + "/json/"))
        .timeout(Duration.ofSeconds(5))
        .GET()
        .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
        throw new IllegalStateException("cep_lookup_unavailable");
      }
      return objectMapper.readValue(response.body(), ViaCepResponse.class);
    } catch (EntityNotFoundException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new IllegalStateException("cep_lookup_unavailable");
    }
  }

  private String normalizeCep(String value) {
    String digits = (value == null ? "" : value).replaceAll("\\D", "");
    if (digits.length() != 8) throw new IllegalArgumentException("entidade_endereco_cep_invalid");
    return digits;
  }

  private String normalizeUf(String value) {
    String normalized = trim(value, 2);
    if (normalized == null) return null;
    normalized = normalized.toUpperCase();
    if (!normalized.matches("[A-Z]{2}")) return null;
    return normalized;
  }

  private String trim(String value, int maxLength) {
    if (value == null) return null;
    String normalized = value.trim();
    if (normalized.isEmpty()) return null;
    if (normalized.length() > maxLength) return normalized.substring(0, maxLength);
    return normalized;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record ViaCepResponse(
    String cep,
    String logradouro,
    String complemento,
    String bairro,
    String localidade,
    String uf,
    String ibge,
    Boolean erro
  ) {}
}
