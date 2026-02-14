package com.ia.app.web;

import com.ia.app.dto.AccessControlPolicyRequest;
import com.ia.app.dto.AccessControlPolicyResponse;
import com.ia.app.service.AccessControlPolicyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access-controls")
public class AccessControlPolicyController {

  private final AccessControlPolicyService service;

  public AccessControlPolicyController(AccessControlPolicyService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public List<AccessControlPolicyResponse> list() {
    return service.list();
  }

  @PutMapping("/{controlKey}")
  @PreAuthorize("hasAnyRole('MASTER','ADMIN')")
  public AccessControlPolicyResponse upsert(@PathVariable String controlKey,
      @Valid @RequestBody AccessControlPolicyRequest request) {
    return service.upsert(controlKey, request.roles());
  }

  @DeleteMapping("/{controlKey}")
  @PreAuthorize("hasAnyRole('MASTER','ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable String controlKey) {
    service.delete(controlKey);
    return ResponseEntity.noContent().build();
  }
}


