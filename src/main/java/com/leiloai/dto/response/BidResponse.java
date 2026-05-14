// src/main/java/com/leiloai/dto/response/BidResponse.java
package com.leiloai.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.leiloai.domain.Bid;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class BidResponse {

    private UUID id;
    private BigDecimal amount;
    private UUID bidderId;
    private String bidderName;
    private UUID auctionId;
    private OffsetDateTime createdAt;

    // Construtor vazio
    public BidResponse() {
    }

    /**
     * Construtor que converte a entidade Bid em DTO de resposta.
     * NUNCA expõe dados sensíveis do usuário (email, password hash).
     */
    public BidResponse(Bid bid) {
        this.id = bid.getId();
        this.amount = bid.getAmount();
        this.bidderId = bid.getBidder().getId();
        this.bidderName = bid.getBidder().getName();
        this.auctionId = bid.getAuction().getId();
        this.createdAt = bid.getCreatedAt();
    }

    // Getters e Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public UUID getBidderId() {
        return bidderId;
    }

    public void setBidderId(UUID bidderId) {
        this.bidderId = bidderId;
    }

    public String getBidderName() {
        return bidderName;
    }

    public void setBidderName(String bidderName) {
        this.bidderName = bidderName;
    }

    public UUID getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(UUID auctionId) {
        this.auctionId = auctionId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}