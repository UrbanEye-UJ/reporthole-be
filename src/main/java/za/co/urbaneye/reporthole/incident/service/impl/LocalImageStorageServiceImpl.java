package za.co.urbaneye.reporthole.incident.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.incident.service.interfaces.ImageStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.UUID;

@Service
public class LocalImageStorageServiceImpl implements ImageStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public String saveBase64Image(String base64Image) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = UUID.randomUUID() + ".jpg";
            Path filePath = uploadPath.resolve(fileName);

            Files.write(filePath, imageBytes, StandardOpenOption.CREATE_NEW);

            return baseUrl + "/uploads/incidents/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("Failed to save image", e);
        }
    }
}
