package com.voicebridge.service.impl;

import com.voicebridge.exception.BusinessRuleViolationException;
import com.voicebridge.exception.ResourceNotFoundException;
import com.voicebridge.service.PresentationStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class LocalPresentationStorageService implements PresentationStorageService {

    private final Path baseStoragePath;

    public LocalPresentationStorageService(@Value("${app.presentation.storage-dir:uploads/presentations}") String storageDir) {
        this.baseStoragePath = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseStoragePath);
        } catch (IOException e) {
            log.error("Could not create base presentation storage directory", e);
            throw new BusinessRuleViolationException("Failed to initialize presentation storage");
        }
    }

    @Override
    public String storePresentationFile(MultipartFile file, String uniqueId) {
        Path presentationDir = baseStoragePath.resolve(uniqueId).normalize();
        if (!presentationDir.startsWith(baseStoragePath)) {
            throw new BusinessRuleViolationException("Invalid storage path attempt");
        }

        try {
            Files.createDirectories(presentationDir);
            Path slidesDir = presentationDir.resolve("slides");
            Files.createDirectories(slidesDir);

            String originalName = sanitizeFilename(file.getOriginalFilename());
            Path targetPath = presentationDir.resolve(originalName).normalize();
            if (!targetPath.startsWith(presentationDir)) {
                throw new BusinessRuleViolationException("Invalid filename path traversal attempt");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Stored presentation file at {}", targetPath);
            return presentationDir.toString();
        } catch (IOException e) {
            log.error("Failed to store presentation file", e);
            throw new BusinessRuleViolationException("Failed to store presentation file");
        }
    }

    @Override
    public void saveSlideImage(byte[] imageBytes, String storagePath, int slideNumber) {
        Path presentationDir = Paths.get(storagePath).toAbsolutePath().normalize();
        Path slidesDir = presentationDir.resolve("slides");
        Path slideFile = slidesDir.resolve("slide_" + slideNumber + ".png");

        try {
            Files.createDirectories(slidesDir);
            Files.write(slideFile, imageBytes);
        } catch (IOException e) {
            log.error("Failed to save slide image for slide {}", slideNumber, e);
            throw new BusinessRuleViolationException("Failed to save slide image");
        }
    }

    @Override
    public byte[] loadSlideImage(String storagePath, int slideNumber) {
        Path presentationDir = Paths.get(storagePath).toAbsolutePath().normalize();
        Path slideFile = presentationDir.resolve("slides").resolve("slide_" + slideNumber + ".png");

        if (!Files.exists(slideFile)) {
            throw new ResourceNotFoundException("Slide " + slideNumber + " asset not found");
        }

        try {
            return Files.readAllBytes(slideFile);
        } catch (IOException e) {
            log.error("Failed to read slide image for slide {}", slideNumber, e);
            throw new BusinessRuleViolationException("Failed to read slide image");
        }
    }

    @Override
    public InputStream getPresentationInputStream(String storagePath, String originalFilename) {
        Path presentationDir = Paths.get(storagePath).toAbsolutePath().normalize();
        Path file = presentationDir.resolve(sanitizeFilename(originalFilename));

        if (!Files.exists(file)) {
            throw new ResourceNotFoundException("Presentation source file not found");
        }

        try {
            return new FileInputStream(file.toFile());
        } catch (IOException e) {
            log.error("Failed to open presentation stream for {}", file, e);
            throw new BusinessRuleViolationException("Failed to open presentation file");
        }
    }

    @Override
    public void deletePresentationDirectory(String storagePath) {
        Path presentationDir = Paths.get(storagePath).toAbsolutePath().normalize();
        if (presentationDir.startsWith(baseStoragePath) && Files.exists(presentationDir)) {
            try {
                FileSystemUtils.deleteRecursively(presentationDir);
                log.info("Deleted presentation directory {}", presentationDir);
            } catch (IOException e) {
                log.warn("Failed to delete presentation directory {}", presentationDir, e);
            }
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "presentation";
        }
        return Paths.get(filename).getFileName().toString();
    }
}
