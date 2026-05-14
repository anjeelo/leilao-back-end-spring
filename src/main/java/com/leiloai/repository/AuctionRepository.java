// src/main/java/com/leiloai/repository/AuctionRepository.java
package com.leiloai.repository;

import com.leiloai.domain.Auction;
import com.leiloai.domain.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, UUID> {

    /**
     * Busca leilões por status.
     * Usado para listar leilões abertos, fechados ou cancelados.
     */
    List<Auction> findByStatus(AuctionStatus status);

    /**
     * Busca leilões de um vendedor específico.
     * Parâmetro nomeado pelo Spring Data — protegido contra SQL Injection.
     */
    List<Auction> findBySellerId(UUID sellerId);

    /**
     * Busca leilões abertos cuja data de término já passou.
     * Usado para job de fechamento automático.
     *
     * @Query com parâmetros nomeados (:status, :now) — seguro contra SQL Injection.
     */
    @Query("SELECT a FROM Auction a WHERE a.status = :status AND a.endDate < :now")
    List<Auction> findOpenAuctionsToClose(@Param("status") AuctionStatus status,
                                          @Param("now") OffsetDateTime now);

    /**
     * Busca leilões por intervalo de datas.
     * Útil para filtrar leilões que começam ou terminam em determinado período.
     */
    @Query("SELECT a FROM Auction a WHERE a.startDate >= :start AND a.endDate <= :end")
    List<Auction> findByDateRange(@Param("start") OffsetDateTime start,
                                  @Param("end") OffsetDateTime end);

    /**
     * Verifica se um vendedor tem leilões ativos.
     */
    boolean existsBySellerIdAndStatus(UUID sellerId, AuctionStatus status);
}