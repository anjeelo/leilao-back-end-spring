// src/main/java/com/leiloai/security/TokenBlacklistService.java
package com.leiloai.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);

    /**
     * Mapa em memória que armazena tokens revogados.
     * Chave: JWT ID (jti) do token
     * Valor: Data de expiração do token original
     *
     * ConcurrentHashMap: thread-safe para ambiente multi-thread.
     */
    private final Map<String, Date> blacklistedTokens = new ConcurrentHashMap<>();

    /**
     * Adiciona um token à blacklist.
     * Armazena o jti (JWT ID) e a data de expiração do token.
     *
     * @param jti  Identificador único do token (claim jti)
     * @param expiration  Data de expiração original do token
     */
    public void blacklist(String jti, Date expiration) {
        blacklistedTokens.put(jti, expiration);
        log.info("Token adicionado à blacklist. Expira em: {}", expiration);
    }

    /**
     * Verifica se um token está na blacklist.
     *
     * @param token Token JWT completo
     * @return true se o token foi revogado
     */
    public boolean isBlacklisted(String token) {
        // Extraímos o jti sem validar assinatura —
        // se o token está adulterado, será rejeitado depois pelo JwtService
        try {
            String jti = extractJtiUnsafe(token);
            if (jti != null && blacklistedTokens.containsKey(jti)) {
                return true;
            }
        } catch (Exception e) {
            // Token malformado — não está na blacklist
            log.debug("Não foi possível extrair jti do token: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Remove tokens expirados da blacklist.
     * Chamado periodicamente para evitar acúmulo de memória.
     * Pode ser agendado com @Scheduled.
     */
    public void cleanup() {
        Date now = new Date();
        blacklistedTokens.entrySet().removeIf(entry -> {
            boolean expired = entry.getValue().before(now);
            if (expired) {
                log.debug("Removendo token expirado da blacklist: {}", entry.getKey());
            }
            return expired;
        });
    }

    /**
     * Extrai o jti (JWT ID) do token sem validar assinatura.
     * Uso interno apenas para verificação de blacklist.
     *
     * A validação real da assinatura é feita pelo JwtService.
     */
    private String extractJtiUnsafe(String token) {
        // Remove o prefixo "Bearer " se presente
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // Decodifica o payload (segunda parte do JWT) sem validar
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return null;
        }

        try {
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            // Extrai o jti manualmente do JSON
            // Procura por "jti":"valor"
            int jtiIndex = payload.indexOf("\"jti\"");
            if (jtiIndex == -1) {
                return null;
            }
            int startQuote = payload.indexOf("\"", jtiIndex + 5);
            int endQuote = payload.indexOf("\"", startQuote + 1);
            if (startQuote == -1 || endQuote == -1) {
                return null;
            }
            return payload.substring(startQuote + 1, endQuote);
        } catch (Exception e) {
            log.debug("Erro ao decodificar payload do token: {}", e.getMessage());
            return null;
        }
    }
}