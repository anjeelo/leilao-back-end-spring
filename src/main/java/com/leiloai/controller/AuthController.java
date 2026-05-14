// src/main/java/com/leiloai/controller/AuthController.java
package com.leiloai.controller;

import com.leiloai.dto.request.LoginRequest;
import com.leiloai.dto.request.RegisterRequest;
import com.leiloai.dto.response.AuthResponse;
import com.leiloai.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registra um novo usuário.
     * POST /auth/register
     * Rota pública — não requer autenticação.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Requisição de registro recebida");
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Autentica um usuário e retorna tokens JWT.
     * POST /auth/login
     * Rota pública — não requer autenticação.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Requisição de login recebida");
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Renova o access token usando um refresh token.
     * POST /auth/refresh
     * Rota pública — refresh token é enviado no corpo.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken) {
        log.info("Requisição de renovação de token recebida");

        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new AuthResponse("Token não informado ou formato inválido")
            );
        }

        String refreshToken = bearerToken.substring(7);
        AuthResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(response);
    }

    /**
     * Invalida o access token (logout).
     * POST /auth/logout
     * Rota autenticada — requer token JWT válido.
     */
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String bearerToken) {
        log.info("Requisição de logout recebida");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String accessToken = bearerToken.substring(7);
            authService.logout(accessToken);
        }

        return ResponseEntity.ok(new AuthResponse("Logout realizado com sucesso"));
    }
}