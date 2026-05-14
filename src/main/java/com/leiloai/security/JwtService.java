// src/main/java/com/leiloai/security/JwtService.java
package com.leiloai.security;

import com.leiloai.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final JwtConfig jwtConfig;
    private final SecretKey signingKey;

    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        // JJWT 0.12.5: usa Keys.hmacShaKeyFor, NÃO Keys.secretKeyFor (depreciado)
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Gera um token de acesso (access token) para o usuário.
     * Curta duração (padrão: 15 minutos).
     */
    public String generateAccessToken(String email, String role, UUID userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        claims.put("userId", userId.toString());
        return buildToken(claims, email, jwtConfig.getAccessTokenExpirationMillis());
    }

    /**
     * Gera um refresh token para renovação do access token.
     * Longa duração (padrão: 24 horas).
     */
    public String generateRefreshToken(String email) {
        return buildToken(new HashMap<>(), email, jwtConfig.getRefreshTokenExpirationMillis());
    }

    /**
     * Extrai o email (subject) do token.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrai o userId do token.
     */
    public String extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", String.class));
    }

    /**
     * Extrai a role do token.
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * Valida se o token é válido e pertence ao usuário informado.
     */
    public boolean isTokenValid(String token, String userEmail) {
        try {
            String extractedEmail = extractEmail(token);
            boolean isValid = extractedEmail.equals(userEmail) && !isTokenExpired(token);
            if (!isValid) {
                log.warn("Token inválido ou expirado para usuário: {}", userEmail);
            }
            return isValid;
        } catch (JwtException e) {
            log.warn("Falha na validação do token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrai a data de expiração do token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // --- Métodos privados ---

    private String buildToken(Map<String, Object> claims, String subject, long expirationMillis) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(jwtConfig.getIssuer())
                .issuedAt(now)
                .expiration(expiration)
                .id(UUID.randomUUID().toString()) // jti: identificador único do token
                .signWith(signingKey)
                .compact();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .clockSkewSeconds(30) // tolerância de 30s para diferença de relógio
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }
}