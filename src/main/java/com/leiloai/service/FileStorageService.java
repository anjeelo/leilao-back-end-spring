// src/main/java/com/leiloai/service/FileStorageService.java
package com.leiloai.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path uploadDir;

    // Tipos MIME permitidos (apenas imagens)
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    // Extensões correspondentes aos MIME types permitidos
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".webp", ".gif"
    );

    // Tamanho máximo por arquivo: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDirPath) {
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
    }

    /**
     * Cria o diretório de upload se não existir.
     * Executado após a injeção de dependências.
     */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(uploadDir);
            log.info("Diretório de upload criado/verificado: {}", uploadDir);
        } catch (IOException e) {
            log.error("Erro ao criar diretório de upload: {}", uploadDir, e);
            throw new RuntimeException("Não foi possível criar o diretório de upload", e);
        }
    }

    /**
     * Salva um arquivo de imagem de forma segura.
     *
     * @param file Arquivo enviado pelo cliente
     * @return Nome do arquivo salvo (UUID + extensão original)
     */
    public String store(MultipartFile file) {
        // Validação 1: Arquivo não pode estar vazio
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("O arquivo está vazio");
        }

        // Validação 2: Tamanho máximo
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("Arquivo muito grande. Máximo permitido: %dMB", MAX_FILE_SIZE / (1024 * 1024))
            );
        }

        // Validação 3: Tipo MIME real do arquivo (não confia na extensão)
        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            log.warn("Tipo MIME não permitido: {} - Arquivo: {}", contentType, originalFilename);
            throw new IllegalArgumentException(
                    "Tipo de arquivo não permitido. Formatos aceitos: JPG, PNG, WebP, GIF"
            );
        }

        // Validação 4: Verifica a extensão do arquivo (camada adicional)
        String extension = getFileExtension(originalFilename);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            log.warn("Extensão não permitida: {} - Arquivo: {}", extension, originalFilename);
            throw new IllegalArgumentException(
                    "Extensão de arquivo não permitida. Formatos aceitos: JPG, PNG, WebP, GIF"
            );
        }

        // Validação 5: Verifica os primeiros bytes do arquivo (magic bytes)
        if (!isValidImageContent(file)) {
            log.warn("Conteúdo do arquivo não corresponde a uma imagem válida: {}", originalFilename);
            throw new IllegalArgumentException(
                    "O arquivo enviado não é uma imagem válida"
            );
        }

        // Gera nome único com UUID — nunca usa o nome original enviado pelo cliente
        String newFilename = UUID.randomUUID().toString() + extension.toLowerCase();

        // Salva o arquivo no disco
        try {
            Path destinationPath = uploadDir.resolve(newFilename);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Arquivo salvo com sucesso: {} -> {}", originalFilename, newFilename);
            return newFilename;
        } catch (IOException e) {
            log.error("Erro ao salvar arquivo: {}", originalFilename, e);
            throw new RuntimeException("Erro ao salvar o arquivo. Tente novamente.", e);
        }
    }

    /**
     * Remove um arquivo do storage.
     *
     * @param filename Nome do arquivo a ser removido
     */
    public void delete(String filename) {
        try {
            Path filePath = uploadDir.resolve(filename).normalize();

            // Segurança: garante que o arquivo está dentro do diretório de upload
            if (!filePath.startsWith(uploadDir)) {
                log.warn("Tentativa de path traversal detectada: {}", filename);
                throw new IllegalArgumentException("Nome de arquivo inválido");
            }

            Files.deleteIfExists(filePath);
            log.info("Arquivo removido: {}", filename);
        } catch (IOException e) {
            log.warn("Erro ao remover arquivo: {}", filename, e);
        }
    }

    /**
     * Extrai a extensão do nome do arquivo.
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return null;
        }
        return filename.substring(dotIndex);
    }

    /**
     * Verifica os magic bytes do arquivo para confirmar que é uma imagem real.
     * Não confia apenas no Content-Type enviado pelo cliente.
     */
    private boolean isValidImageContent(MultipartFile file) {
        try {
            byte[] header = new byte[8];
            try (InputStream is = file.getInputStream()) {
                int bytesRead = is.read(header);
                if (bytesRead < 4) {
                    return false;
                }
            }

            // JPEG: FF D8 FF
            if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
                return true;
            }

            // PNG: 89 50 4E 47
            if (header[0] == (byte) 0x89 && header[1] == (byte) 0x50
                    && header[2] == (byte) 0x4E && header[3] == (byte) 0x47) {
                return true;
            }

            // GIF: 47 49 46 38
            if (header[0] == (byte) 0x47 && header[1] == (byte) 0x49
                    && header[2] == (byte) 0x46 && header[3] == (byte) 0x38) {
                return true;
            }

            // WebP: 52 49 46 46 ... 57 45 42 50
            if (header[0] == (byte) 0x52 && header[1] == (byte) 0x49
                    && header[2] == (byte) 0x46 && header[3] == (byte) 0x46
                    && header.length >= 8
                    && header[4] == (byte) 0x57 && header[5] == (byte) 0x45
                    && header[6] == (byte) 0x42 && header[7] == (byte) 0x50) {
                return true;
            }

            return false;
        } catch (IOException e) {
            log.warn("Erro ao verificar magic bytes do arquivo", e);
            return false;
        }
    }
}