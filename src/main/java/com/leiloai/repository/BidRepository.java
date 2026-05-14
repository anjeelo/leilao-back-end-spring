// src/main/java/com/leiloai/repository/BidRepository.java
package com.leiloai.repository;

import com.leiloai.domain.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BidRepository extends JpaRepository<Bid, UUID> {

    /**
     * Busca todos os lances de um leilão, ordenados do maior para o menor valor.
     * Usado para exibir o histórico de lances.
     */
    List<Bid> findByAuctionIdOrderByAmountDesc(UUID auctionId);

    /**
     * Busca o maior lance de um leilão.
     * Usado para verificar se um novo lance supera o valor atual.
     *
     * @Query com parâmetro nomeado :auctionId — protegido contra SQL Injection.
     */
    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.amount DESC LIMIT 1")
    Optional<Bid> findTopByAuctionIdOrderByAmountDesc(@Param("auctionId") UUID auctionId);

    /**
     * Busca todos os lances de um usuário.
     * Útil para histórico de lances do usuário.
     */
    List<Bid> findByBidderIdOrderByCreatedAtDesc(UUID bidderId);

    /**
     * Conta quantos lances um usuário fez em um leilão específico.
     * Útil para regras de negócio (ex.: limite de lances por usuário).
     */
    long countByAuctionIdAndBidderId(UUID auctionId, UUID bidderId);

    /**
     * Verifica se um usuário já deu algum lance em um leilão.
     */
    boolean existsByAuctionIdAndBidderId(UUID auctionId, UUID bidderId);
}