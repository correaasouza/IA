package com.ia.app.service;

import com.ia.app.domain.ConfigFormulario;
import com.ia.app.dto.ConfigRequest;
import com.ia.app.dto.ConfigResponse;
import com.ia.app.repository.ConfigFormularioRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ConfigFormularioService {

  private final ConfigFormularioRepository repository;

  public ConfigFormularioService(ConfigFormularioRepository repository) {
    this.repository = repository;
  }

  @Cacheable(value = "configFormulario", key = "#tenantId + ':' + #screenId + ':' + #userId + ':' + #rolesKey")
  public ConfigResponse resolve(Long tenantId, String screenId, String userId, String rolesKey, List<String> roles) {
    return findConfig(tenantId, screenId, userId, roles);
  }

  public java.time.Instant maxUpdatedAt(Long tenantId, String screenId) {
    return repository.findMaxUpdatedAt(tenantId, screenId);
  }

  @CacheEvict(value = "configFormulario", allEntries = true)
  public ConfigFormulario save(ConfigRequest request, Long tenantId) {
    ConfigFormulario entity = repository.findFirstByTenantIdAndScreenIdAndScopeTipoAndScopeValor(
      tenantId, request.screenId(), request.scopeTipo(), request.scopeValor()).orElse(new ConfigFormulario());

    entity.setTenantId(tenantId);
    entity.setScreenId(request.screenId());
    entity.setScopeTipo(request.scopeTipo());
    entity.setScopeValor(request.scopeValor());
    entity.setConfigJson(request.configJson());
    entity.setVersao(entity.getVersao() == null ? 1 : entity.getVersao() + 1);
    return repository.save(entity);
  }

  private ConfigResponse findConfig(Long tenantId, String screenId, String userId, List<String> roles) {
    if (userId != null) {
      var userCfg = repository.findFirstByTenantIdAndScreenIdAndScopeTipoAndScopeValor(
        tenantId, screenId, "USER", userId);
      if (userCfg.isPresent()) {
        return new ConfigResponse("USER", userCfg.get().getConfigJson());
      }
    }

    if (roles != null && !roles.isEmpty()) {
      var roleCfg = repository.findFirstByTenantIdAndScreenIdAndScopeTipoAndScopeValorIn(
        tenantId, screenId, "ROLE", roles);
      if (roleCfg.isPresent()) {
        return new ConfigResponse("ROLE", roleCfg.get().getConfigJson());
      }
    }

    var tenantCfg = repository.findFirstByTenantIdAndScreenIdAndScopeTipoAndScopeValor(
      tenantId, screenId, "TENANT", String.valueOf(tenantId));
    if (tenantCfg.isPresent()) {
      return new ConfigResponse("TENANT", tenantCfg.get().getConfigJson());
    }

    var defaultCfg = repository.findFirstByTenantIdAndScreenIdAndScopeTipo(0L, screenId, "DEFAULT");
    if (defaultCfg.isPresent()) {
      return new ConfigResponse("DEFAULT", defaultCfg.get().getConfigJson());
    }

    return new ConfigResponse("NONE", "{}");
  }
}
