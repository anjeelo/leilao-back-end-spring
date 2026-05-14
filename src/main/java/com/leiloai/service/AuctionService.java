// src/main/java/com/leiloai/service/AuctionService.java
package com.leiloai.service;

import com.leiloai.domain.Auction;
import com.leiloai.domain.AuctionStatus;
import com.leiloai.domain.User;
import com.leiloai.dto.request.CreateAuctionRequest;
import com.leiloai.dto.response.AuctionResponse;
import com.leiloai.exception.AuctionNotFoundException;
import com.leiloai.exception.UnauthorizedException;
import com.leiloai.repository.AuctionRepository;
import com.leiloai.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public AuctionService(AuctionRepository auctionRepository,
                          UserRepository userRepository,
                          FileStorageService fileStorageService) {
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Cria um novo leilão.
     * Apenas usuários autenticados podem criar leilões.
     *
     * @param request Dados do leilão
     * @param sellerEmail Email do usuário autenticado (extraído do JWT)
     * @return AuctionResponse com os dados do leilão criado
     */
    @Transactional
    public AuctionResponse create(CreateAuctionRequest request, String sellerEmail) {
        log.info("Criando leilão: {} por usuário: {}", request.getTitle(), sellerEmail);

        // Busca o vendedor pelo email do token JWT
        User seller = userRepository.findByEmailIgnoreCase(sellerEmail)
                .orElseThrow(() -> {
                    log.error("Usuário autenticado não encontrado: {}", sellerEmail);
                    return new UnauthorizedException("Usuário não encontrado");
                });

        // Valida se data de término é posterior à data de início
        if (request.getEndDate().isBefore(request.getStartDate()) ||
                request.getEndDate().isEqual(request.getStartDate())) {
            throw new IllegalArgumentException(
                    "A data de término deve ser posterior à data de início"
            );
        }

        // Cria a entidade
        Auction auction = new Auction(
                request.getTitle(),
                request.getDescription(),
                request.getInitialPrice(),
                request.getStartDate(),
                request.getEndDate(),
                seller
        );

        auction = auctionRepository.save(auction);
        log.info("Leilão criado com sucesso: id={}", auction.getId());

        return new AuctionResponse(auction);
    }

    /**
     * Busca um leilão por ID.
     * Público para consulta, mas dados sensíveis são filtrados no DTO.
     */
    @Transactional(readOnly = true)
    public AuctionResponse findById(UUID id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new AuctionNotFoundException("Leilão não encontrado com ID: " + id));
        return new AuctionResponse(auction);
    }

    /**
     * Lista leilões por status.
     *
     * @param status Status dos leilões (OPEN, CLOSED, CANCELLED)
     */
    @Transactional(readOnly = true)
    public List<AuctionResponse> findByStatus(AuctionStatus status) {
        log.info("Buscando leilões com status: {}", status);
        return auctionRepository.findByStatus(status)
                .stream()
                .map(AuctionResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Lista leilões de um vendedor específico.
     */
    @Transactional(readOnly = true)
    public List<AuctionResponse> findBySeller(UUID sellerId) {
        log.info("Buscando leilões do vendedor: {}", sellerId);
        return auctionRepository.findBySellerId(sellerId)
                .stream()
                .map(AuctionResponse::new)
                .collect(Collectors.toList());
    }

    /**
     * Cancela um leilão.
     * Apenas o vendedor do leilão pode cancelá-lo.
     * Apenas leilões com status OPEN podem ser cancelados.
     *
     * @param auctionId ID do leilão
     * @param userEmail Email do usuário autenticado (extraído do JWT)
     */
    @Transactional
    public AuctionResponse cancel(UUID auctionId, String userEmail) {
        log.info("Tentativa de cancelar leilão: {} por usuário: {}", auctionId, userEmail);

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Leilão não encontrado com ID: " + auctionId));

        // Verifica se o usuário é o vendedor
        if (!auction.getSeller().getEmail().equalsIgnoreCase(userEmail)) {
            log.warn("Usuário {} tentou cancelar leilão de outro vendedor: {}", userEmail, auctionId);
            throw new UnauthorizedException("Apenas o vendedor pode cancelar este leilão");
        }

        // Verifica se o leilão está aberto
        if (auction.getStatus() != AuctionStatus.OPEN) {
            throw new IllegalArgumentException("Apenas leilões abertos podem ser cancelados");
        }

        auction.setStatus(AuctionStatus.CANCELLED);
        auction = auctionRepository.save(auction);

        log.info("Leilão cancelado com sucesso: {}", auctionId);
        return new AuctionResponse(auction);
    }

    /**
     * Fecha um leilão e define o vencedor.
     * Método chamado por job automático ou manualmente pelo vendedor.
     *
     * @param auctionId ID do leilão
     * @param userEmail Email do usuário autenticado
     */
    @Transactional
    public AuctionResponse close(UUID auctionId, String userEmail) {
        log.info("Tentativa de fechar leilão: {} por usuário: {}", auctionId, userEmail);

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Leilão não encontrado com ID: " + auctionId));

        // Verifica se o usuário é o vendedor
        if (!auction.getSeller().getEmail().equalsIgnoreCase(userEmail)) {
            log.warn("Usuário {} tentou fechar leilão de outro vendedor: {}", userEmail, auctionId);
            throw new UnauthorizedException("Apenas o vendedor pode fechar este leilão");
        }

        // Verifica se o leilão está aberto
        if (auction.getStatus() != AuctionStatus.OPEN) {
            throw new IllegalArgumentException("Apenas leilões abertos podem ser fechados");
        }

        auction.setStatus(AuctionStatus.CLOSED);
        auction = auctionRepository.save(auction);

        log.info("Leilão fechado com sucesso: {}", auctionId);
        return new AuctionResponse(auction);
    }

    /**
     * Faz upload de uma imagem para um leilão.
     * Apenas o vendedor do leilão pode fazer upload.
     *
     * @param auctionId ID do leilão
     * @param file Arquivo de imagem
     * @param userEmail Email do usuário autenticado
     * @return Nome do arquivo salvo
     */
    @Transactional
    public String uploadImage(UUID auctionId, MultipartFile file, String userEmail) {
        log.info("Tentativa de upload de imagem para leilão: {} por: {}", auctionId, userEmail);

        // Busca o leilão
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Leilão não encontrado com ID: " + auctionId));

        // Verifica se o usuário é o vendedor
        if (!auction.getSeller().getEmail().equalsIgnoreCase(userEmail)) {
            log.warn("Usuário {} tentou fazer upload em leilão de outro vendedor: {}", userEmail, auctionId);
            throw new UnauthorizedException("Apenas o vendedor pode fazer upload de imagens neste leilão");
        }

        // Verifica se o leilão está aberto
        if (auction.getStatus() != AuctionStatus.OPEN) {
            throw new IllegalArgumentException("Apenas leilões abertos podem receber upload de imagens");
        }

        // Se já existe uma imagem, remove a antiga
        if (auction.getImageFilename() != null) {
            fileStorageService.delete(auction.getImageFilename());
        }

        // Salva o novo arquivo
        String filename = fileStorageService.store(file);
        auction.setImageFilename(filename);
        auctionRepository.save(auction);

        log.info("Upload de imagem realizado com sucesso: {} -> {}", auctionId, filename);
        return filename;
    }

    /**
     * Remove a imagem de um leilão.
     * Apenas o vendedor do leilão pode remover.
     *
     * @param auctionId ID do leilão
     * @param userEmail Email do usuário autenticado
     */
    @Transactional
    public void deleteImage(UUID auctionId, String userEmail) {
        log.info("Tentativa de remoção de imagem do leilão: {} por: {}", auctionId, userEmail);

        // Busca o leilão
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new AuctionNotFoundException("Leilão não encontrado com ID: " + auctionId));

        // Verifica se o usuário é o vendedor
        if (!auction.getSeller().getEmail().equalsIgnoreCase(userEmail)) {
            log.warn("Usuário {} tentou remover imagem de leilão de outro vendedor: {}", userEmail, auctionId);
            throw new UnauthorizedException("Apenas o vendedor pode remover a imagem deste leilão");
        }

        // Remove a imagem se existir
        if (auction.getImageFilename() != null) {
            fileStorageService.delete(auction.getImageFilename());
            auction.setImageFilename(null);
            auctionRepository.save(auction);
            log.info("Imagem removida com sucesso do leilão: {}", auctionId);
        } else {
            log.info("Nenhuma imagem para remover no leilão: {}", auctionId);
        }
    }
}