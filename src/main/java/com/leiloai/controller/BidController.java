// src/main/java/com/leiloai/controller/BidController.java
package com.leiloai.controller;

import com.leiloai.dto.request.PlaceBidRequest;
import com.leiloai.dto.response.BidResponse;
import com.leiloai.service.BidService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auctions/{auctionId}/bids")
public class BidController {

    private static final Logger log = LoggerFactory.getLogger(BidController.class);

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    /**
     * Registra um lance em um leilão.
     * POST /auctions/{auctionId}/bids
     * Requer autenticação.
     */
    @PostMapping
    public ResponseEntity<BidResponse> placeBid(
            @PathVariable UUID auctionId,
            @Valid @RequestBody PlaceBidRequest request) {
        String userEmail = getAuthenticatedUserEmail();
        log.info("Requisição de lance no leilão {} por: {}", auctionId, userEmail);
        BidResponse response = bidService.placeBid(auctionId, request, userEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista todos os lances de um leilão.
     * GET /auctions/{auctionId}/bids
     * Requer autenticação.
     */
    @GetMapping
    public ResponseEntity<List<BidResponse>> findByAuction(@PathVariable UUID auctionId) {
        log.info("Requisição para listar lances do leilão: {}", auctionId);
        List<BidResponse> response = bidService.findByAuction(auctionId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lista todos os lances de um usuário.
     * GET /users/{userId}/bids
     * Requer autenticação.
     */
    @GetMapping("/users/{userId}/bids")
    public ResponseEntity<List<BidResponse>> findByBidder(@PathVariable UUID userId) {
        log.info("Requisição para listar lances do usuário: {}", userId);
        List<BidResponse> response = bidService.findByBidder(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Extrai o email do usuário autenticado do contexto de segurança.
     */
    private String getAuthenticatedUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new com.leiloai.exception.UnauthorizedException("Usuário não autenticado");
        }
        return authentication.getName();
    }
}