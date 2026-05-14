// src/main/java/com/leiloai/dto/response/UserResponse.java
package com.leiloai.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.leiloai.domain.User;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    private UUID id;
    private String name;
    private String email;
    private String role;
    private Boolean enabled;
    private OffsetDateTime createdAt;

    // Construtor vazio
    public UserResponse() {
    }

    /**
     * Construtor que converte a entidade User em DTO de resposta.
     * NUNCA expõe passwordHash.
     */
    public UserResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.role = user.getRole().name();
        this.enabled = user.getEnabled();
        this.createdAt = user.getCreatedAt();
    }

    // Getters e Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}