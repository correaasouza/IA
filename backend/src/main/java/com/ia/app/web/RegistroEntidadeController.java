package com.ia.app.web;

import com.ia.app.dto.RegistroEntidadeRequest;
import com.ia.app.dto.RegistroEntidadeResponse;
import com.ia.app.service.RegistroEntidadeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registros-entidade")
public class RegistroEntidadeController {

  private final RegistroEntidadeService service;

  public RegistroEntidadeController(RegistroEntidadeService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN','USER')")
  public ResponseEntity<Page<RegistroEntidadeResponse>> list(
      @RequestParam Long tipoEntidadeId,
      Pageable pageable) {
    Page<RegistroEntidadeResponse> page = service.list(tipoEntidadeId, pageable)
      .map(registro -> new RegistroEntidadeResponse(registro.getId(), registro.getTipoEntidadeId(), Map.of()));

    List<Long> ids = page.getContent().stream().map(RegistroEntidadeResponse::id).toList();
    Map<Long, Map<String, Object>> valores = ids.isEmpty() ? Map.of() : service.loadValores(ids);

    Page<RegistroEntidadeResponse> mapped = page.map(item -> new RegistroEntidadeResponse(
      item.id(),
      item.tipoEntidadeId(),
      valores.getOrDefault(item.id(), Map.of())
    ));

    return ResponseEntity.ok(mapped);
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('MASTER_ADMIN','TENANT_ADMIN')")
  public ResponseEntity<RegistroEntidadeResponse> create(@Valid @RequestBody RegistroEntidadeRequest request) {
    var registro = service.create(request);
    return ResponseEntity.ok(new RegistroEntidadeResponse(registro.getId(), registro.getTipoEntidadeId(), Map.of()));
  }
}
