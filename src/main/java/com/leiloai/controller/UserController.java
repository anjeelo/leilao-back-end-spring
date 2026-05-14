// src/main/java/com/leiloai/controller/UserController.java
package com.leiloai.controller;

import com.leiloai.dto.response.UserResponse;
import com.leiloai.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Retorna o perfil do usuário autenticado.
     * GET /users/me
     * Requer autenticação.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile() {
        String userEmail = getAuthenticatedUserEmail();
        log.info("Requisição para buscar perfil do usuário: {}", userEmail);
        UserResponse response = userService.getProfile(userEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * Busca um usuário por ID.
     * GET /users/{id}
     * Requer autenticação. Apenas ADMIN ou o próprio usuário.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        String userEmail = getAuthenticatedUserEmail();
        log.info("Requisição para buscar usuário: {} por: {}", id, userEmail);
        UserResponse response = userService.findById(id, userEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * Lista todos os usuários.
     * GET /users
     * Requer autenticação. Apenas ADMIN.
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> findAll() {
        String userEmail = getAuthenticatedUserEmail();
        log.info("Requisição para listar todos os usuários por: {}", userEmail);
        List<UserResponse> response = userService.findAll(userEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * Extrai o email do usuário autenticado do contexto de segurança.
     */
    private String getAuthenticatedUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new com.leiloai.exception.UnauthorizedException("Usuário não autenticado");
        }
        return authentication.getName();
    }
}