// src/main/java/com/leiloai/service/AuthService.java
package com.leiloai.service;

import com.leiloai.config.JwtConfig;
import com.leiloai.domain.User;
import com.leiloai.dto.request.LoginRequest;
import com.leiloai.dto.request.RegisterRequest;
import com.leiloai.dto.response.AuthResponse;
import com.leiloai.repository.UserRepository;
import com.leiloai.security.JwtService;
import com.leiloai.security.TokenBlacklistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtConfig jwtConfig,
                       TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtConfig = jwtConfig;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * Registra um novo usuário.
     *
     * @throws IllegalArgumentException se o email já existe
     */
    public AuthResponse register(RegisterRequest request) {
        log.info("Tentativa de registro para email: {}", request.getEmail());

        // Verifica se email já existe (case-insensitive)
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            log.warn("Tentativa de registro com email duplicado: {}", request.getEmail());
            throw new IllegalArgumentException("Já existe um usuário com este email");
        }

        // Cria usuário com senha hasheada
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        log.info("Usuário registrado com sucesso: {}", user.getEmail());

        // Gera tokens para login automático após registro
        String accessToken = jwtService.generateAccessToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId()
        );
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtConfig.getAccessTokenExpirationMillis() / 1000
        );
    }

    /**
     * Autentica um usuário e retorna tokens JWT.
     *
     * @throws BadCredentialsException se credenciais forem inválidas
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Tentativa de login para email: {}", request.getEmail());

        // Busca usuário por email (case-insensitive)
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login falhou: email não encontrado: {}", request.getEmail());
                    return new BadCredentialsException("Email ou senha inválidos");
                });

        // Verifica se conta está ativa
        if (!user.getEnabled()) {
            log.warn("Login falhou: conta desativada: {}", user.getEmail());
            throw new BadCredentialsException("Conta desativada. Entre em contato com o suporte.");
        }

        // Verifica senha
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login falhou: senha incorreta para: {}", user.getEmail());
            throw new BadCredentialsException("Email ou senha inválidos");
        }

        log.info("Login bem-sucedido: {}", user.getEmail());

        // Gera tokens
        String accessToken = jwtService.generateAccessToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId()
        );
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtConfig.getAccessTokenExpirationMillis() / 1000
        );
    }

    /**
     * Renova o access token usando um refresh token válido.
     *
     * @throws BadCredentialsException se o refresh token for inválido
     */
    public AuthResponse refresh(String refreshToken) {
        log.info("Tentativa de renovação de token");

        // Extrai email do refresh token
        String email;
        try {
            email = jwtService.extractEmail(refreshToken);
        } catch (Exception e) {
            log.warn("Refresh token inválido: não foi possível extrair email");
            throw new BadCredentialsException("Refresh token inválido");
        }

        // Busca usuário
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> {
                    log.warn("Refresh token inválido: usuário não encontrado: {}", email);
                    return new BadCredentialsException("Usuário não encontrado");
                });

        // Verifica se conta está ativa
        if (!user.getEnabled()) {
            log.warn("Refresh token rejeitado: conta desativada: {}", email);
            throw new BadCredentialsException("Conta desativada");
        }

        // Valida o refresh token
        if (!jwtService.isTokenValid(refreshToken, user.getEmail())) {
            log.warn("Refresh token expirado ou inválido para: {}", email);
            throw new BadCredentialsException("Refresh token inválido ou expirado");
        }

        log.info("Token renovado com sucesso para: {}", email);

        // Gera novo access token (refresh token NÃO é renovado aqui)
        String newAccessToken = jwtService.generateAccessToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId()
        );

        return new AuthResponse(
                newAccessToken,
                refreshToken, // mesmo refresh token
                jwtConfig.getAccessTokenExpirationMillis() / 1000
        );
    }

    /**
     * Invalida o access token (logout).
     * Adiciona o token à blacklist.
     */
    public void logout(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }

        try {
            String jti = jwtService.extractEmail(accessToken); // fallback
            java.util.Date expiration = jwtService.extractExpiration(accessToken);

            // Extrai o jti do token para blacklist
            String tokenJti = extractJtiFromRawToken(accessToken);
            if (tokenJti != null && expiration != null) {
                tokenBlacklistService.blacklist(tokenJti, expiration);
                log.info("Token invalidado com sucesso: jti={}", tokenJti);
            }
        } catch (Exception e) {
            log.warn("Erro ao invalidar token: {}", e.getMessage());
            // Não lança exceção — o token pode já estar expirado
        }
    }

    /**
     * Extrai o jti do token JWT raw (sem validação).
     * Método auxiliar interno.
     */
    private String extractJtiFromRawToken(String token) {
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
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
            return null;
        }
    }
}