// src/main/java/com/leiloai/dto/request/CreateAuctionRequest.java
package com.leiloai.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class CreateAuctionRequest {

    @NotBlank(message = "O título é obrigatório")
    @Size(min = 3, max = 200, message = "O título deve ter entre 3 e 200 caracteres")
    private String title;

    @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
    private String description;

    @NotNull(message = "O preço inicial é obrigatório")
    @DecimalMin(value = "0.01", message = "O preço inicial deve ser maior que zero")
    private BigDecimal initialPrice;

    @NotNull(message = "A data de início é obrigatória")
    @Future(message = "A data de início deve ser no futuro")
    private OffsetDateTime startDate;

    @NotNull(message = "A data de término é obrigatória")
    @Future(message = "A data de término deve ser no futuro")
    private OffsetDateTime endDate;

    // Construtores
    public CreateAuctionRequest() {
    }

    public CreateAuctionRequest(String title, String description, BigDecimal initialPrice,
                                OffsetDateTime startDate, OffsetDateTime endDate) {
        this.title = title;
        this.description = description;
        this.initialPrice = initialPrice;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters e Setters
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
}