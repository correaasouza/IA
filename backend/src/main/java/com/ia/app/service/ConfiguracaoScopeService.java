package com.ia.app.service;

import com.ia.app.repository.ConfigColunaRepository;
import com.ia.app.repository.ConfigFormularioRepository;
import com.ia.app.repository.CatalogConfigurationRepository;
import com.ia.app.repository.TipoEntidadeRepository;
import java.util.Locale;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ConfiguracaoScopeService {
  public static final String TYPE_FORMULARIO = "FORMULARIO";
  public static final String TYPE_COLUNA = "COLUNA";
  public static final String TYPE_TIPO_ENTIDADE = "TIPO_ENTIDADE";
  public static final String TYPE_CATALOGO = "CATALOGO";

  private final ConfigFormularioRepository configFormularioRepository;
  private final ConfigColunaRepository configColunaRepository;
  private final TipoEntidadeRepository tipoEntidadeRepository;
  private final CatalogConfigurationRepository catalogConfigurationRepository;

  public ConfiguracaoScopeService(
      ConfigFormularioRepository configFormularioRepository,
      ConfigColunaRepository configColunaRepository,
      TipoEntidadeRepository tipoEntidadeRepository,
      CatalogConfigurationRepository catalogConfigurationRepository) {
    this.configFormularioRepository = configFormularioRepository;
    this.configColunaRepository = configColunaRepository;
    this.tipoEntidadeRepository = tipoEntidadeRepository;
    this.catalogConfigurationRepository = catalogConfigurationRepository;
  }

  public String normalizeAndValidate(Long tenantId, String configType, Long configId) {
    if (configId == null || configId <= 0) {
      throw new IllegalArgumentException("config_id_invalid");
    }
    if (configType == null || configType.isBlank()) {
      throw new IllegalArgumentException("config_type_invalid");
    }
    String normalized = configType.trim().toUpperCase(Locale.ROOT);
    boolean exists = switch (normalized) {
      case TYPE_FORMULARIO -> configFormularioRepository.existsByIdAndTenantId(configId, tenantId);
      case TYPE_COLUNA -> configColunaRepository.existsByIdAndTenantId(configId, tenantId);
      case TYPE_TIPO_ENTIDADE -> tipoEntidadeRepository.existsByIdAndTenantIdAndAtivoTrue(configId, tenantId);
      case TYPE_CATALOGO -> catalogConfigurationRepository.existsByIdAndTenantId(configId, tenantId);
      default -> false;
    };
    if (!exists) {
      if (!TYPE_FORMULARIO.equals(normalized)
        && !TYPE_COLUNA.equals(normalized)
        && !TYPE_TIPO_ENTIDADE.equals(normalized)
        && !TYPE_CATALOGO.equals(normalized)) {
        throw new IllegalArgumentException("config_type_invalid");
      }
      throw new EntityNotFoundException("config_not_found");
    }
    return normalized;
  }
}
