// src/main/java/com/leiloai/dto/response/AuctionResponse.java
package com.leiloai.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.leiloai.domain.Auction;
import com.leiloai.domain.AuctionStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuctionResponse {

    private UUID id;
    private String title;
    private String description;
    private BigDecimal initialPrice;
    private BigDecimal currentPrice;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private AuctionStatus status;
    private UUID sellerId;
    private String sellerName;
    private UUID winnerId;
    private String winnerName;
    private String imageFilename;
    private OffsetDateTime createdAt;

    // Construtor vazio
    public AuctionResponse() {
    }

    /**
     * Construtor que converte a entidade Auction em DTO de resposta.
     * NUNCA expõe dados sensíveis do usuário (password hash, email).
     */
    public AuctionResponse(Auction auction) {
        this.id = auction.getId();
        this.title = auction.getTitle();
        this.description = auction.getDescription();
        this.initialPrice = auction.getInitialPrice();
        this.currentPrice = auction.getCurrentPrice();
        this.startDate = auction.getStartDate();
        this.endDate = auction.getEndDate();
        this.status = auction.getStatus();
        this.sellerId = auction.getSeller().getId();
        this.sellerName = auction.getSeller().getName();
        this.winnerId = auction.getWinner() != null ? auction.getWinner().getId() : null;
        this.winnerName = auction.getWinner() != null ? auction.getWinner().getName() : null;
        this.imageFilename = auction.getImageFilename();
        this.createdAt = auction.getCreatedAt();
    }

    // Getters e Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getInitialPrice() {
        return initialPrice;
    }

    public void setInitialPrice(BigDecimal initialPrice) {
        this.initialPrice = initialPrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;
    }

    public OffsetDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(OffsetDateTime endDate) {
        this.endDate = endDate;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public void setSellerId(UUID sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public UUID getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(UUID winnerId) {
        this.winnerId = winnerId;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    public String getImageFilename() {
        return imageFilename;
    }

    public void setImageFilename(String imageFilename) {
        this.imageFilename = imageFilename;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}