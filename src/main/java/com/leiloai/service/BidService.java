// src/main/java/com/leiloai/service/BidService.java
package com.leiloai.service;

import com.leiloai.domain.Auction;
import com.leiloai.domain.AuctionStatus;
import com.leiloai.domain.Bid;
import com.leiloai.domain.User;
import com.leiloai.dto.request.PlaceBidRequest;
import com.leiloai.dto.response.BidResponse;
import com.leiloai.exception.AuctionNotFoundException;
import com.leiloai.exception.UnauthorizedException;
import com.leiloai.repository.AuctionRepository;
import com.leiloai.repository.BidRepository;
import com.leiloai.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BidService {

    private static final Logger log = LoggerFactory.getLogger(BidService.class);

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    public BidService(BidRepository bidRepository,
                      AuctionRepository auctionRepository,
                      UserRepository userRepository) {
        this.bidRepository = bidRepository;
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
    }

    /**
     * Registra um lance em um leilão.
     *
     * @param auctionId ID do leilão
     * @param request Dados do lance (valor)
     * @param bidderEmail Email do usuário autenticado (extraído do JWT)
     * @return BidResponse com os dados do lance registrado
     */
    @Transactional
    public BidResponse placeBid(UUID auctionId, PlaceBidRequest request, String bidderEmail) {
        log.info("Tentativa de lance no leilão {} por usuário: {}", auctionId, bidderEmail);

        // Busca o leilão
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Leilão não encontrado com ID: " + auctionId));

        // Busca o usuário pelo email do token JWT
        User bidder = userRepository.findByEmailIgnoreCase(bidderEmail)
                .orElseThrow(() -> {
                    log.error("Usuário autenticado não encontrado: {}", bidderEmail);
                    return new UnauthorizedException("Usuário não encontrado");
                });

        // Validações de negócio
        validateBid(auction, request.getAmount(), bidder);

        // Cria o lance
        Bid bid = new Bid(request.getAmount(), bidder, auction);
        bid = bidRepository.save(bid);

        // Atualiza o preço atual do leilão
        auction.setCurrentPrice(request.getAmount());
        auctionRepository.save(auction);

        log.info("Lance registrado com sucesso: id={}, valor={}, leilão={}",
                bid.getId(), bid.getAmount(), auctionId);

        return new BidResponse(bid);
    }

    /**
     * Lista todos os lances de um leilão, do maior para o menor.
     */
    @Transactional(readOnly = true)
    public List<BidResponse> findByAuction(UUID auctionId) {
        log.info("Buscando lances do leilão: {}", auctionId);

        // Verifica se o leilão existe
        if (!auctionRepository.existsById(auctionId)) {
            throw new AuctionNotFoundException("Leilão não encontrado com ID: " + auctionId);
        }

        return bidRepository.findByAuctionIdOrderByAmountDesc(auctionId)
                .stream()
                .map(BidResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Lista todos os lances de um usuário.
     */
    @Transactional(readOnly = true)
    public List<BidResponse> findByBidder(UUID bidderId) {
        log.info("Buscando lances do usuário: {}", bidderId);

        return bidRepository.findByBidderIdOrderByCreatedAtDesc(bidderId)
                .stream()
                .map(BidResponse::new)
                .collect(Collectors.toList());
    }

    // --- Validações privadas ---

    /**
     * Valida se um lance pode ser registrado.
     * Regras:
     * 1. Leilão deve estar aberto
     * 2. Leilão deve estar dentro do período válido
     * 3. Vendedor não pode dar lance no próprio leilão
     * 4. Valor do lance deve ser maior que o preço atual
     */
    private void validateBid(Auction auction, BigDecimal amount, User bidder) {
        // Regra 1: Leilão deve estar aberto
        if (auction.getStatus() != AuctionStatus.OPEN) {
            throw new IllegalArgumentException(
                    "Este leilão não está aberto para lances. Status atual: " + auction.getStatus()
            );
        }

        // Regra 2: Leilão deve estar dentro do período válido
        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(auction.getStartDate())) {
            throw new IllegalArgumentException("Este leilão ainda não começou");
        }
        if (now.isAfter(auction.getEndDate())) {
            throw new IllegalArgumentException("Este leilão já foi encerrado");
        }

        // Regra 3: Vendedor não pode dar lance no próprio leilão
        if (auction.getSeller().getId().equals(bidder.getId())) {
            throw new IllegalArgumentException("O vendedor não pode dar lance no próprio leilão");
        }

        // Regra 4: Valor do lance deve ser maior que o preço atual
        if (amount.compareTo(auction.getCurrentPrice()) <= 0) {
            throw new IllegalArgumentException(
                    String.format("O lance deve ser maior que o valor atual (R$ %.2f)",
                            auction.getCurrentPrice())
            );
        }
    }
}