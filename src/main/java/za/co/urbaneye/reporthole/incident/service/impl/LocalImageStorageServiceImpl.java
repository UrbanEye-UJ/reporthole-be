package za.co.urbaneye.reporthole.incident.service.impl;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import za.co.urbaneye.reporthole.incident.service.interfaces.ImageStorageService;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.UUID;

@Service
public class LocalImageStorageServiceImpl implements ImageStorageService {

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${server.port:8080}")
    private int serverPort;

    @Value("${services.web.base-url:}")
    private String configuredBaseUrl;

    private String resolvedBaseUrl;

    @PostConstruct
    void resolveBaseUrl() {
        if (configuredBaseUrl != null && !configuredBaseUrl.isBlank()) {
            resolvedBaseUrl = configuredBaseUrl;
        } else {
            String ip = detectLocalIp();
            resolvedBaseUrl = "http://" + ip + ":" + serverPort + "/api";
        }
        System.out.println("[ImageStorage] Resolved base URL: " + resolvedBaseUrl);
    }

    private String detectLocalIp() {
        try {
            // Prefer the address that can actually reach the internet
            try (java.net.DatagramSocket socket = new java.net.DatagramSocket()) {
                socket.connect(java.net.InetAddress.getByName("8.8.8.8"), 80);
                String ip = socket.getLocalAddress().getHostAddress();
                if (ip != null && !ip.isBlank() && !"0.0.0.0".equals(ip)) return ip;
            } catch (Exception ignored) {}

            // Fallback: first non-loopback, non-virtual IPv4
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(interfaces)) {
                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;
                for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "localhost";
    }

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

            return resolvedBaseUrl + "/uploads/incidents/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("Failed to save image", e);
        }
    }
}
