// src/main/java/com/leiloai/service/UserService.java
package com.leiloai.service;

import com.leiloai.domain.User;
import com.leiloai.dto.response.UserResponse;
import com.leiloai.exception.UnauthorizedException;
import com.leiloai.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Busca o perfil do usuário autenticado.
     *
     * @param userEmail Email do usuário autenticado (extraído do JWT)
     * @return UserResponse com dados do perfil
     */
    @Transactional(readOnly = true)
    public UserResponse getProfile(String userEmail) {
        log.info("Buscando perfil do usuário: {}", userEmail);

        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> {
                    log.error("Usuário autenticado não encontrado: {}", userEmail);
                    return new UnauthorizedException("Usuário não encontrado");
                });

        return new UserResponse(user);
    }

    /**
     * Busca um usuário por ID.
     * Apenas ADMIN pode buscar qualquer usuário.
     *
     * @param userId ID do usuário
     * @param requesterEmail Email de quem fez a requisição
     * @return UserResponse com dados do usuário
     */
    @Transactional(readOnly = true)
    public UserResponse findById(UUID userId, String requesterEmail) {
        log.info("Usuário {} buscando dados do usuário: {}", requesterEmail, userId);

        // Busca o solicitante para verificar se é ADMIN
        User requester = userRepository.findByEmailIgnoreCase(requesterEmail)
                .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado"));

        // Apenas ADMIN pode ver dados de outros usuários
        if (!requester.getRole().name().equals("ADMIN") &&
                !requester.getId().equals(userId)) {
            log.warn("Usuário {} tentou acessar dados de outro usuário: {}", requesterEmail, userId);
            throw new UnauthorizedException("Você não tem permissão para acessar este perfil");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado com ID: " + userId));

        return new UserResponse(user);
    }

    /**
     * Lista todos os usuários.
     * Apenas ADMIN pode listar.
     *
     * @param requesterEmail Email de quem fez a requisição
     * @return Lista de UserResponse
     */
    @Transactional(readOnly = true)
    public List<UserResponse> findAll(String requesterEmail) {
        log.info("Usuário {} solicitando lista de todos os usuários", requesterEmail);

        // Busca o solicitante para verificar se é ADMIN
        User requester = userRepository.findByEmailIgnoreCase(requesterEmail)
                .orElseThrow(() -> new UnauthorizedException("Usuário não encontrado"));

        if (!requester.getRole().name().equals("ADMIN")) {
            log.warn("Usuário não-ADMIN {} tentou listar todos os usuários", requesterEmail);
            throw new UnauthorizedException("Apenas administradores podem listar todos os usuários");
        }

        return userRepository.findAll()
                .stream()
                .map(UserResponse::new)
                .collect(Collectors.toList());
    }
}