// src/main/java/com/leiloai/repository/UserRepository.java
package com.leiloai.repository;

import com.leiloai.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Busca usuário por email (ignora case).
     * Usado no login e no registro para verificar duplicidade.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Verifica se já existe um usuário com determinado email.
     * Usa parâmetro nomeado :email — protegido contra SQL Injection.
     */
    boolean existsByEmailIgnoreCase(String email);
}