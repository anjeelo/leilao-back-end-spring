// src/main/java/com/leiloai/controller/AuctionController.java
package com.leiloai.controller;

import com.leiloai.domain.AuctionStatus;
import com.leiloai.dto.request.CreateAuctionRequest;
import com.leiloai.dto.response.AuctionResponse;
import com.leiloai.service.AuctionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auctions")
public class AuctionController {

    private static final Logger log = LoggerFactory.getLogger(AuctionController.class);

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    /**
     * Cria um novo leilão.
     * POST /auctions
     * Requer autenticação.
     */
    @PostMapping
    public ResponseEntity<AuctionResponse> create(@Valid @RequestBody CreateAuctionRequest request) {
        String userEmail = getAuthenticatedUserEmail();
        log.info("Requisição para criar leilão recebida de: {}", userEmail);
        AuctionResponse response = auctionService.create(request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Busca um leilão por ID.
     * GET /auctions/{id}
     * Requer autenticação.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AuctionResponse> findById(@PathVariable UUID id) {
        log.info("Requisição para buscar leilão: {}", id);
        AuctionResponse response = auctionService.findById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Lista leilões por status.
     * GET /auctions?status=OPEN
     * Requer autenticação.
     */
    @GetMapping
    public ResponseEntity<List<AuctionResponse>> findByStatus(
            @RequestParam(defaultValue = "OPEN") String status) {
        log.info("Requisição para listar leilões com status: {}", status);

        AuctionStatus auctionStatus;
        try {
            auctionStatus = AuctionStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Status inválido. Valores permitidos: OPEN, CLOSED, CANCELLED"
            );
        }

        List<AuctionResponse> response = auctionService.findByStatus(auctionStatus);
        return ResponseEntity.ok(response);
    }

    /**
     * Lista leilões de um vendedor específico.
     * GET /auctions/seller/{sellerId}
     * Requer autenticação.
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<List<AuctionResponse>> findBySeller(@PathVariable UUID sellerId) {
        log.info("Requisição para listar leilões do vendedor: {}", sellerId);
        List<AuctionResponse> response = auctionService.findBySeller(sellerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancela um leilão.
     * PATCH /auctions/{id}/cancel
     * Requer autenticação. Apenas o vendedor pode cancelar.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AuctionResponse> cancel(@PathVariable UUID id) {
        String userEmail = getAuthenticatedUserEmail();
        log.info("Requisição para cancelar leilão: {} por: {}", id, userEmail);
        AuctionResponse response = auctionService.cancel(id, userEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * Fecha um leilão manualmente.
     * PATCH /auctions/{id}/close
     * Requer autenticação. Apenas o vendedor pode fechar.
     */
    @PatchMapping("/{id}/close")
    public ResponseEntity<AuctionResponse> close(@PathVariable UUID id) {
        String userEmail = getAuthenticatedUserEmail();
        log.info("Requisição para fechar leilão: {} por: {}", id, userEmail);
        AuctionResponse response = auctionService.close(id, userEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * Faz upload de uma imagem para um leilão.
     * POST /auctions/{id}/image
     * Requer autenticação. Apenas o vendedor pode fazer upload.
     */
    @PostMapping("/{id}/image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) {
        String userEmail = getAuthenticatedUserEmail();
        log.info("Requisição de upload de imagem para leilão: {} por: {}", id, userEmail);

        String filename = auctionService.uploadImage(id, file, userEmail);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Imagem enviada com sucesso");
        response.put("filename", filename);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Remove uma imagem de um leilão.
     * DELETE /auctions/{id}/image
     * Requer autenticação. Apenas o vendedor pode remover.
     */
    @DeleteMapping("/{id}/image")
    public ResponseEntity<Map<String, String>> deleteImage(@PathVariable UUID id) {
        String userEmail = getAuthenticatedUserEmail();
        log.info("Requisição de remoção de imagem do leilão: {} por: {}", id, userEmail);

        auctionService.deleteImage(id, userEmail);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Imagem removida com sucesso");
        return ResponseEntity.ok(response);
    }

    /**
     * Extrai o email do usuário autenticado do contexto de segurança.
     * O email é o subject do JWT e também o username no UserDetails.
     */
    private String getAuthenticatedUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new com.leiloai.exception.UnauthorizedException("Usuário não autenticado");
        }
        return authentication.getName();
    }
}