package com.ia.app.web;

import com.ia.app.dto.AtalhoUsuarioOrdemRequest;
import com.ia.app.dto.AtalhoUsuarioRequest;
import com.ia.app.dto.AtalhoUsuarioResponse;
import com.ia.app.mapper.AtalhoUsuarioMapper;
import com.ia.app.service.AtalhoUsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atalhos")
public class AtalhoUsuarioController {

  private final AtalhoUsuarioService service;

  public AtalhoUsuarioController(AtalhoUsuarioService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<AtalhoUsuarioResponse>> list(Authentication authentication) {
    String userId = getUserId(authentication);
    return ResponseEntity.ok(service.list(userId).stream().map(AtalhoUsuarioMapper::toResponse).toList());
  }

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<AtalhoUsuarioResponse> create(Authentication authentication,
      @Valid @RequestBody AtalhoUsuarioRequest request) {
    String userId = getUserId(authentication);
    return ResponseEntity.ok(AtalhoUsuarioMapper.toResponse(service.create(userId, request)));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> delete(Authentication authentication, @PathVariable Long id) {
    String userId = getUserId(authentication);
    service.delete(userId, id);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/ordem")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> reorder(Authentication authentication,
      @Valid @RequestBody List<AtalhoUsuarioOrdemRequest> ordens) {
    String userId = getUserId(authentication);
    service.reorder(userId, ordens);
    return ResponseEntity.noContent().build();
  }

  private String getUserId(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
      return "unknown";
    }
    return jwt.getSubject();
  }
}
