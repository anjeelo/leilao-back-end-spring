// src/main/java/com/leiloai/config/JwtConfig.java
package com.leiloai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /**
     * Chave secreta para assinatura HMAC-SHA256 do JWT.
     * Deve ter pelo menos 256 bits (32 caracteres) para HS256.
     */
    private String secret;

    /**
     * Tempo de expiração do token de acesso em MINUTOS.
     * Padrão: 15 minutos (curta duração por segurança).
     */
    private int accessTokenExpiration = 15;

    /**
     * Tempo de expiração do refresh token em MINUTOS.
     * Padrão: 1440 minutos (24 horas).
     */
    private int refreshTokenExpiration = 1440;

    /**
     * Emissor (iss) declarado nos tokens JWT gerados.
     */
    private String issuer = "leiloai-api";

    // Getters e Setters
    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public void setAccessTokenExpiration(int accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public int getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    public void setRefreshTokenExpiration(int refreshTokenExpiration) {
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * Retorna a expiração do access token em MILISSEGUNDOS.
     * Útil para o JwtService que trabalha com millis.
     */
    public long getAccessTokenExpirationMillis() {
        return (long) accessTokenExpiration * 60 * 1000;
    }

    /**
     * Retorna a expiração do refresh token em MILISSEGUNDOS.
     */
    public long getRefreshTokenExpirationMillis() {
        return (long) refreshTokenExpiration * 60 * 1000;
    }
}