// ApiClient.java - HTTP client for communicating with central API
package com.monitoring.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitoring.api.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;

public class ApiClient {
    private static final Logger logger = LoggerFactory.getLogger(ApiClient.class);
    
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    
    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = System.getenv("MONITORING_API_KEY");
    }
    
    // Container Registration
    public boolean registerContainer(ContainerRegistration registration) throws Exception {
        String json = objectMapper.writeValueAsString(registration);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/containers/register"))
                .header("Content-Type", "application/json")
                .header("X-API-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(30))
                .build();
        
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                ApiResponse apiResponse = objectMapper.readValue(response.body(), ApiResponse.class);
                return apiResponse.isSuccess();
            } else {
                logger.error("Registration failed with status: {}, body: {}", 
                           response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error during container registration", e);
            throw e;
        }
    }
    
    // Heartbeat
    public void sendHeartbeat(String containerId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/containers/" + containerId + "/heartbeat"))
                .header("Content-Type", "application/json")
                .header("X-API-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .timeout(Duration.ofSeconds(10))
                .build();
        
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                logger.warn("Heartbeat failed with status: {}", response.statusCode());
            }
        } catch (Exception e) {
            logger.warn("Error sending heartbeat for container: {}", containerId, e);
            throw e;
        }
    }
    
    // Command Polling
    public Command pollForCommand(String containerId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/containers/" + containerId + "/commands/poll"))
                .header("X-API-Key", apiKey)
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();
        
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), Command.class);
            } else if (response.statusCode() == 204) {
                // No content - no commands available
                return null;
            } else {
                logger.warn("Command polling failed with status: {}", response.statusCode());
                return null;
            }
        } catch (Exception e) {
            logger.warn("Error polling for commands for container: {}", containerId, e);
            throw e;
        }
    }
    
    // Send Command Result
    public void sendCommandResult(CommandResult result) throws Exception {
        String json = objectMapper.writeValueAsString(result);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/containers/" + result.getContainerId() + "/commands/result"))
                .header("Content-Type", "application/json")
                .header("X-API-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(30))
                .build();
        
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                logger.error("Failed to send command result with status: {}, body: {}", 
                           response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.error("Error sending command result for container: {}", result.getContainerId(), e);
            throw e;
        }
    }
    
    // File Upload
    public String uploadFile(File file, String fileType) throws Exception {
        // Create multipart form data
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, "UTF-8"), true);
        
        // File part
        writer.append("--" + boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"").append("\r\n");
        writer.append("Content-Type: application/octet-stream").append("\r\n");
        writer.append("\r\n");
        writer.flush();
        
        // Write file content
        baos.write(Files.readAllBytes(file.toPath()));
        
        writer.append("\r\n");
        
        // File type part
        writer.append("--" + boundary).append("\r\n");
        writer.append("Content-Disposition: form-data; name=\"fileType\"").append("\r\n");
        writer.append("\r\n");
        writer.append(fileType);
        writer.append("\r\n");
        
        writer.append("--" + boundary + "--").append("\r\n");
        writer.flush();
        
        byte[] multipartData = baos.toByteArray();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/containers/" + getCurrentContainerId() + "/files/upload"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("X-API-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartData))
                .timeout(Duration.ofMinutes(5)) // Longer timeout for file upload
                .build();
        
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                FileUploadResponse uploadResponse = objectMapper.readValue(response.body(), FileUploadResponse.class);
                return uploadResponse.getFileId();
            } else {
                logger.error("File upload failed with status: {}, body: {}", 
                           response.statusCode(), response.body());
                throw new RuntimeException("File upload failed");
            }
        } catch (Exception e) {
            logger.error("Error uploading file: {}", file.getName(), e);
            throw e;
        }
    }
    
    // Health Check
    public boolean isHealthy() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
            
        } catch (Exception e) {
            logger.debug("Health check failed", e);
            return false;
        }
    }
    
    private String getCurrentContainerId() {
        // This should be set from the container listener
        return System.getProperty("container.id", "unknown");
    }
}

// FileService.java - Service for handling file operations
package com.monitoring.api.service;

import com.monitoring.api.model.FileInfo;
import com.monitoring.api.model.GeneratedFile;
import com.monitoring.api.repository.GeneratedFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FileService {
    
    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    
    @Autowired
    private GeneratedFileRepository fileRepository;
    
    @Value("${file.storage.path:/app/storage}")
    private String fileStoragePath;
    
    @Value("${file.storage.max-size:1GB}")
    private String maxFileSize;
    
    private Path fileStorageLocation;
    
    public void init() {
        this.fileStorageLocation = Paths.get(fileStoragePath).toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }
    
    public String storeFile(String containerId, MultipartFile file, String fileType) throws IOException {
        // Normalize file name
        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        
        if (fileName.contains("..")) {
            throw new IllegalArgumentException("Sorry! Filename contains invalid path sequence " + fileName);
        }
        
        // Generate unique file ID and path
        String fileId = UUID.randomUUID().toString();
        String fileExtension = getFileExtension(fileName);
        String storedFileName = fileId + fileExtension;
        
        // Resolve the file location
        Path targetLocation = this.fileStorageLocation.resolve(storedFileName);
        
        // Copy file to the target location
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        
        // Save file info to database
        GeneratedFile generatedFile = new GeneratedFile();
        generatedFile.setContainerId(containerId);
        generatedFile.setFileType(fileType);
        generatedFile.setFilePath(targetLocation.toString());
        generatedFile.setFileSize(file.getSize());
        generatedFile.setCreatedAt(LocalDateTime.now());
        generatedFile.setExpiresAt(LocalDateTime.now().plusDays(7)); // Files expire after 7 days
        
        GeneratedFile savedFile = fileRepository.save(generatedFile);
        
        logger.info("File stored successfully: {} for container: {}", storedFileName, containerId);
        
        return savedFile.getId().toString();
    }
    
    public Resource loadFileAsResource(String fileId) throws Exception {
        try {
            GeneratedFile generatedFile = fileRepository.findById(Long.valueOf(fileId))
                    .orElseThrow(() -> new RuntimeException("File not found with id: " + fileId));
            
            // Check if file has expired
            if (generatedFile.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("File has expired");
            }
            
            Path filePath = Paths.get(generatedFile.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return resource;
            } else {
                throw new RuntimeException("File not found on filesystem: " + generatedFile.getFilePath());
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("File not found with id: " + fileId, ex);
        }
    }
    
    public FileInfo getFileInfo(String fileId) {
        try {
            GeneratedFile generatedFile = fileRepository.findById(Long.valueOf(fileId))
                    .orElse(null);
            
            if (generatedFile == null) {
                return null;
            }
            
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileId(fileId);
            fileInfo.setFileName(Paths.get(generatedFile.getFilePath()).getFileName().toString());
            fileInfo.setFileType(generatedFile.getFileType());
            fileInfo.setFileSize(generatedFile.getFileSize());
            fileInfo.setCreatedAt(generatedFile.getCreatedAt());
            fileInfo.setExpiresAt(generatedFile.getExpiresAt());
            
            return fileInfo;
        } catch (Exception e) {
            logger.error("Error retrieving file info for ID: {}", fileId, e);
            return null;
        }
    }
    
    public void cleanupExpiredFiles() {
        try {
            // Find expired files
            var expiredFiles = fileRepository.findExpiredFiles(LocalDateTime.now());
            
            for (GeneratedFile expiredFile : expiredFiles) {
                try {
                    // Delete from filesystem
                    Path filePath = Paths.get(expiredFile.getFilePath());
                    Files.deleteIfExists(filePath);
                    
                    // Delete from database
                    fileRepository.delete(expiredFile);
                    
                    logger.info("Cleaned up expired file: {}", expiredFile.getFilePath());
                } catch (Exception e) {
                    logger.error("Error cleaning up expired file: {}", expiredFile.getFilePath(), e);
                }
            }
            
            logger.info("Cleanup completed. Removed {} expired files", expiredFiles.size());
            
        } catch (Exception e) {
            logger.error("Error during file cleanup", e);
        }
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        
        return fileName.substring(lastDotIndex);
    }
}

// CleanupApplication.java - Standalone application for scheduled cleanup
package com.monitoring.api;

import com.monitoring.api.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

@SpringBootApplication
@Profile("cleanup")
public class CleanupApplication implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(CleanupApplication.class);
    
    @Autowired
    private FileService fileService;
    
    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "cleanup");
        SpringApplication.run(CleanupApplication.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting file cleanup process...");
        
        try {
            fileService.cleanupExpiredFiles();
            logger.info("File cleanup completed successfully");
        } catch (Exception e) {
            logger.error("File cleanup failed", e);
            System.exit(1);
        }
        
        System.exit(0);
    }
}