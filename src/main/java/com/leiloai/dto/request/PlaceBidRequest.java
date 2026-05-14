// src/main/java/com/leiloai/dto/request/PlaceBidRequest.java
package com.leiloai.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PlaceBidRequest {

    @NotNull(message = "O valor do lance é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor do lance deve ser maior que zero")
    private BigDecimal amount;

    // Construtores
    public PlaceBidRequest() {
    }

    public PlaceBidRequest(BigDecimal amount) {
        this.amount = amount;
    }

    // Getter e Setter
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}