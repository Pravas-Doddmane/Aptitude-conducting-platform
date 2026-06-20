package com.PassFamilyDoddmane.QuizeBackend.service;

import com.PassFamilyDoddmane.QuizeBackend.common.exception.BadRequestException;
import com.PassFamilyDoddmane.QuizeBackend.config.UploadProperties;
import com.PassFamilyDoddmane.QuizeBackend.dto.file.ImageUploadResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final UploadProperties uploadProperties;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(getUploadDir());
    }

    public ImageUploadResponse uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Only image files are allowed");
        }
        String originalName = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String extension = getExtension(originalName);
        String fileName = UUID.randomUUID() + extension;
        Path target = getUploadDir().resolve(fileName);
        Files.copy(file.getInputStream(), target);
        return new ImageUploadResponse("/uploads/" + fileName);
    }

    private Path getUploadDir() {
        String uploadDir = uploadProperties.dir() == null || uploadProperties.dir().isBlank() ? "uploads" : uploadProperties.dir();
        return Path.of(uploadDir).toAbsolutePath().normalize();
    }

    private String getExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        return fileName.substring(index).toLowerCase();
    }
}
