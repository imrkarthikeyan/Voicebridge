package com.voicebridge.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface PresentationStorageService {

    String storePresentationFile(MultipartFile file, String uniqueId);

    void saveSlideImage(byte[] imageBytes, String storagePath, int slideNumber);

    byte[] loadSlideImage(String storagePath, int slideNumber);

    InputStream getPresentationInputStream(String storagePath, String originalFilename);

    void deletePresentationDirectory(String storagePath);
}
