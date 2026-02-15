package com.ia.app.service;

import com.ia.app.repository.ConfigColunaRepository;
import com.ia.app.repository.ConfigFormularioRepository;
import com.ia.app.repository.TipoEntidadeRepository;
import java.util.Locale;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ConfiguracaoScopeService {
  public static final String TYPE_FORMULARIO = "FORMULARIO";
  public static final String TYPE_COLUNA = "COLUNA";
  public static final String TYPE_TIPO_ENTIDADE = "TIPO_ENTIDADE";

  private final ConfigFormularioRepository configFormularioRepository;
  private final ConfigColunaRepository configColunaRepository;
  private final TipoEntidadeRepository tipoEntidadeRepository;

  public ConfiguracaoScopeService(
      ConfigFormularioRepository configFormularioRepository,
      ConfigColunaRepository configColunaRepository,
      TipoEntidadeRepository tipoEntidadeRepository) {
    this.configFormularioRepository = configFormularioRepository;
    this.configColunaRepository = configColunaRepository;
    this.tipoEntidadeRepository = tipoEntidadeRepository;
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
      default -> false;
    };
    if (!exists) {
      if (!TYPE_FORMULARIO.equals(normalized)
        && !TYPE_COLUNA.equals(normalized)
        && !TYPE_TIPO_ENTIDADE.equals(normalized)) {
        throw new IllegalArgumentException("config_type_invalid");
      }
      throw new EntityNotFoundException("config_not_found");
    }
    return normalized;
  }
}
