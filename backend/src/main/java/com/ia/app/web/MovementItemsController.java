package com.ia.app.web;

import com.ia.app.dto.MovementItemsBatchAddRequest;
import com.ia.app.dto.MovementItemsBatchAddResponse;
import com.ia.app.service.MovimentoItemBatchService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movements")
public class MovementItemsController {

  private final MovimentoItemBatchService movimentoItemBatchService;

  public MovementItemsController(MovimentoItemBatchService movimentoItemBatchService) {
    this.movimentoItemBatchService = movimentoItemBatchService;
  }

  @PostMapping("/{movementId}/items")
  @PreAuthorize("@permissaoGuard.hasPermissao('MOVIMENTO_ESTOQUE_ITEM_OPERAR')")
  public ResponseEntity<MovementItemsBatchAddResponse> addItems(
      @PathVariable Long movementId,
      @RequestBody @Valid MovementItemsBatchAddRequest request) {
    return ResponseEntity.ok(movimentoItemBatchService.addItems(movementId, request));
  }
}
