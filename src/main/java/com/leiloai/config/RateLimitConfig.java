// src/main/java/com/leiloai/config/RateLimitConfig.java
package com.leiloai.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Configuração de Rate Limiting por IP.
 * Protege os endpoints de autenticação contra ataques de força bruta.
 *
 * Implementação em memória — em produção, usar Redis ou Bucket4j.
 */
@Configuration
public class RateLimitConfig {

    private static final Logger log = LoggerFactory.getLogger(RateLimitConfig.class);

    /**
     * Mapa thread-safe que armazena a contagem de requisições por IP.
     * Chave: IP do cliente
     * Valor: Timestamp da janela atual + contagem de requisições
     */
    private static final Map<String, RateLimitEntry> requestCounts = new ConcurrentHashMap<>();

    // Configurações de limite
    private static final int MAX_REQUESTS_PER_WINDOW = 10;  // Máximo de requisições
    private static final long WINDOW_DURATION_MS = 60_000;  // Janela de 1 minuto (em ms)

    /**
     * Entrada de rate limit para um IP.
     */
    private static class RateLimitEntry {
        long windowStart;
        int count;

        RateLimitEntry(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }

    /**
     * Retorna um HandlerInterceptor que pode ser registrado manualmente.
     * Uso: registrar no WebMvcConfigurer para rotas específicas.
     */
    public HandlerInterceptor rateLimitInterceptor() {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request,
                                     HttpServletResponse response,
                                     Object handler) throws Exception {
                String clientIp = getClientIp(request);
                long now = System.currentTimeMillis();

                RateLimitEntry entry = requestCounts.compute(clientIp, (ip, existingEntry) -> {
                    if (existingEntry == null || now - existingEntry.windowStart > WINDOW_DURATION_MS) {
                        // Nova janela
                        return new RateLimitEntry(now, 1);
                    } else {
                        // Mesma janela — incrementa
                        existingEntry.count++;
                        return existingEntry;
                    }
                });

                if (entry.count > MAX_REQUESTS_PER_WINDOW) {
                    log.warn("Rate limit excedido para IP: {} — {} requisições em {}ms",
                            clientIp, entry.count, WINDOW_DURATION_MS);

                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType("application/json");
                    response.setHeader("Retry-After", String.valueOf(
                            TimeUnit.MILLISECONDS.toSeconds(WINDOW_DURATION_MS)));
                    response.getWriter().write(
                            "{\"error\":\"Too Many Requests\"," +
                                    "\"message\":\"Muitas requisições. Tente novamente em 1 minuto.\"," +
                                    "\"status\":429}"
                    );
                    return false;
                }

                return true;
            }
        };
    }

    /**
     * Método utilitário público para verificar rate limit programaticamente.
     * Pode ser chamado diretamente nos controllers ou services.
     *
     * @param clientIp IP do cliente
     * @return true se o IP está dentro do limite, false se excedeu
     */
    public boolean tryConsume(String clientIp) {
        long now = System.currentTimeMillis();

        RateLimitEntry entry = requestCounts.compute(clientIp, (ip, existingEntry) -> {
            if (existingEntry == null || now - existingEntry.windowStart > WINDOW_DURATION_MS) {
                return new RateLimitEntry(now, 1);
            } else {
                existingEntry.count++;
                return existingEntry;
            }
        });

        if (entry.count > MAX_REQUESTS_PER_WINDOW) {
            log.warn("Rate limit excedido para IP: {} — {} requisições", clientIp, entry.count);
            return false;
        }

        return true;
    }

    /**
     * Limpa entradas expiradas do mapa periodicamente.
     * Pode ser chamado via @Scheduled para evitar acúmulo de memória.
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        requestCounts.entrySet().removeIf(entry ->
                now - entry.getValue().windowStart > WINDOW_DURATION_MS * 2
        );
        log.debug("Limpeza de rate limit executada. Entradas ativas: {}", requestCounts.size());
    }

    /**
     * Extrai o IP real do cliente, considerando headers de proxy.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Pega o primeiro IP da lista (cliente original)
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }
}