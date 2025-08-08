// ContainerController.java - Central API REST endpoints
package com.monitoring.api.controller;

import com.monitoring.api.model.*;
import com.monitoring.api.service.ContainerService;
import com.monitoring.api.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ContainerController {
    
    private static final Logger logger = LoggerFactory.getLogger(ContainerController.class);
    
    @Autowired
    private ContainerService containerService;
    
    @Autowired
    private FileService fileService;
    
    // Container Registration
    @PostMapping("/containers/register")
    public ResponseEntity<ApiResponse> registerContainer(@RequestBody ContainerRegistration registration) {
        try {
            logger.info("Registering container: {} for team: {}, app: {}", 
                       registration.getContainerId(), registration.getTeamName(), registration.getAppName());
            
            boolean success = containerService.registerContainer(registration);
            if (success) {
                return ResponseEntity.ok(new ApiResponse(true, "Container registered successfully"));
            } else {
                return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Failed to register container"));
            }
        } catch (Exception e) {
            logger.error("Error registering container: " + registration.getContainerId(), e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse(false, "Internal server error: " + e.getMessage()));
        }
    }
    
    // Heartbeat
    @PostMapping("/containers/{containerId}/heartbeat")
    public ResponseEntity<ApiResponse> heartbeat(@PathVariable String containerId) {
        try {
            containerService.updateHeartbeat(containerId);
            return ResponseEntity.ok(new ApiResponse(true, "Heartbeat received"));
        } catch (Exception e) {
            logger.error("Error processing heartbeat for container: " + containerId, e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse(false, "Failed to process heartbeat"));
        }
    }
    
    // Command Polling
    @GetMapping("/containers/{containerId}/commands/poll")
    public ResponseEntity<Command> pollForCommand(@PathVariable String containerId) {
        try {
            Command command = containerService.getNextCommand(containerId);
            if (command != null) {
                logger.debug("Returning command {} for container {}", command.getId(), containerId);
                return ResponseEntity.ok(command);
            } else {
                return ResponseEntity.noContent().build();
            }
        } catch (Exception e) {
            logger.error("Error polling for command for container: " + containerId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Command Result Submission
    @PostMapping("/containers/{containerId}/commands/result")
    public ResponseEntity<ApiResponse> submitCommandResult(
            @PathVariable String containerId, 
            @RequestBody CommandResult result) {
        try {
            logger.info("Received command result for container: {}, command: {}", 
                       containerId, result.getCommandId());
            
            containerService.saveCommandResult(result);
            return ResponseEntity.ok(new ApiResponse(true, "Command result saved"));
        } catch (Exception e) {
            logger.error("Error saving command result for container: " + containerId, e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse(false, "Failed to save command result"));
        }
    }
    
    // GUI Endpoints - Hierarchy Navigation
    @GetMapping("/teams")
    public ResponseEntity<List<String>> getTeams() {
        try {
            List<String> teams = containerService.getAllTeams();
            return ResponseEntity.ok(teams);
        } catch (Exception e) {
            logger.error("Error retrieving teams", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/teams/{teamName}/apps")
    public ResponseEntity<List<String>> getApps(@PathVariable String teamName) {
        try {
            List<String> apps = containerService.getAppsByTeam(teamName);
            return ResponseEntity.ok(apps);
        } catch (Exception e) {
            logger.error("Error retrieving apps for team: " + teamName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/teams/{teamName}/apps/{appName}/pods")
    public ResponseEntity<List<String>> getPods(@PathVariable String teamName,
                                               @PathVariable String appName) {
        try {
            List<String> pods = containerService.getPodsByTeamAndApp(teamName, appName);
            return ResponseEntity.ok(pods);
        } catch (Exception e) {
            logger.error("Error retrieving pods for team: {} app: {}", teamName, appName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/teams/{teamName}/apps/{appName}/pods/{podName}/containers")
    public ResponseEntity<List<ContainerInfo>> getContainers(@PathVariable String teamName,
                                                           @PathVariable String appName,
                                                           @PathVariable String podName) {
        try {
            List<ContainerInfo> containers = containerService.getContainersByPod(teamName, appName, podName);
            return ResponseEntity.ok(containers);
        } catch (Exception e) {
            logger.error("Error retrieving containers for pod: {}", podName, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Command Execution
    @PostMapping("/containers/{containerId}/command")
    public ResponseEntity<ApiResponse> sendCommand(@PathVariable String containerId,
                                                 @RequestBody CommandRequest commandRequest) {
        try {
            logger.info("Sending command {} to container: {}", commandRequest.getType(), containerId);
            
            Long commandId = containerService.queueCommand(containerId, commandRequest);
            
            return ResponseEntity.ok(new ApiResponse(true, 
                "Command queued successfully", 
                "commandId", commandId.toString()));
        } catch (Exception e) {
            logger.error("Error queuing command for container: " + containerId, e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse(false, "Failed to queue command"));
        }
    }
    
    // Container Status
    @GetMapping("/containers/{containerId}/status")
    public ResponseEntity<ContainerStatus> getContainerStatus(@PathVariable String containerId) {
        try {
            ContainerStatus status = containerService.getContainerStatus(containerId);
            if (status != null) {
                return ResponseEntity.ok(status);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving status for container: " + containerId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Command Status and History
    @GetMapping("/commands/{commandId}/status")
    public ResponseEntity<CommandStatus> getCommandStatus(@PathVariable Long commandId) {
        try {
            CommandStatus status = containerService.getCommandStatus(commandId);
            if (status != null) {
                return ResponseEntity.ok(status);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving command status: " + commandId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/containers/{containerId}/commands/history")
    public ResponseEntity<List<CommandHistory>> getCommandHistory(@PathVariable String containerId,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        try {
            List<CommandHistory> history = containerService.getCommandHistory(containerId, page, size);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Error retrieving command history for container: " + containerId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // File Upload (from containers)
    @PostMapping("/containers/{containerId}/files/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(@PathVariable String containerId,
                                                       @RequestParam("file") MultipartFile file,
                                                       @RequestParam("fileType") String fileType) {
        try {
            logger.info("Uploading file from container: {}, type: {}, size: {}", 
                       containerId, fileType, file.getSize());
            
            String fileId = fileService.storeFile(containerId, file, fileType);
            
            FileUploadResponse response = new FileUploadResponse();
            response.setFileId(fileId);
            response.setFileName(file.getOriginalFilename());
            response.setFileSize(file.getSize());
            response.setFileType(fileType);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error uploading file from container: " + containerId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // File Download (for GUI users)
    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId,
                                               HttpServletRequest request) {
        try {
            Resource resource = fileService.loadFileAsResource(fileId);
            
            String contentType = null;
            try {
                contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            } catch (IOException ex) {
                logger.info("Could not determine file type.");
            }
            
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            logger.error("Error downloading file: " + fileId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    // File Info
    @GetMapping("/files/{fileId}/info")
    public ResponseEntity<FileInfo> getFileInfo(@PathVariable String fileId) {
        try {
            FileInfo fileInfo = fileService.getFileInfo(fileId);
            if (fileInfo != null) {
                return ResponseEntity.ok(fileInfo);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving file info: " + fileId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // Health Check
    @GetMapping("/health")
    public ResponseEntity<ApiResponse> health() {
        return ResponseEntity.ok(new ApiResponse(true, "API service is healthy"));
    }
}

// ContainerService.java - Business logic service
package com.monitoring.api.service;

import com.monitoring.api.model.*;
import com.monitoring.api.repository.ContainerRepository;
import com.monitoring.api.repository.CommandRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ContainerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ContainerService.class);
    
    @Autowired
    private ContainerRepository containerRepository;
    
    @Autowired
    private CommandRepository commandRepository;
    
    public boolean registerContainer(ContainerRegistration registration) {
        try {
            // Check if container already exists
            Optional<Container> existingContainer = containerRepository.findById(registration.getContainerId());
            
            Container container;
            if (existingContainer.isPresent()) {
                container = existingContainer.get();
                logger.info("Updating existing container: {}", registration.getContainerId());
            } else {
                container = new Container();
                container.setId(registration.getContainerId());
                container.setCreatedAt(LocalDateTime.now());
                logger.info("Creating new container: {}", registration.getContainerId());
            }
            
            container.setTeamName(registration.getTeamName());
            container.setAppName(registration.getAppName());
            container.setPodName(registration.getPodName());
            container.setContainerName(registration.getContainerName());
            container.setHostIp(registration.getHostIp());
            container.setStatus(registration.getStatus());
            container.setLastHeartbeat(LocalDateTime.now());
            container.setMetadata(registration.getJvmInfo());
            
            containerRepository.save(container);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to register container: " + registration.getContainerId(), e);
            return false;
        }
    }
    
    public void updateHeartbeat(String containerId) {
        Optional<Container> containerOpt = containerRepository.findById(containerId);
        if (containerOpt.isPresent()) {
            Container container = containerOpt.get();
            container.setLastHeartbeat(LocalDateTime.now());
            container.setStatus("active");
            containerRepository.save(container);
        }
    }
    
    public Command getNextCommand(String containerId) {
        List<CommandQueue> pendingCommands = commandRepository.findPendingCommandsByContainerId(containerId);
        
        if (!pendingCommands.isEmpty()) {
            CommandQueue commandQueue = pendingCommands.get(0);
            
            // Mark as processing
            commandQueue.setStatus("processing");
            commandQueue.setExecutedAt(LocalDateTime.now());
            commandRepository.save(commandQueue);
            
            // Convert to Command object
            Command command = new Command();
            command.setId(commandQueue.getId());
            command.setType(commandQueue.getCommandType());
            command.setParameters(commandQueue.getParameters());
            
            return command;
        }
        
        return null;
    }
    
    public void saveCommandResult(CommandResult result) {
        Optional<CommandQueue> commandOpt = commandRepository.findById(result.getCommandId());
        if (commandOpt.isPresent()) {
            CommandQueue command = commandOpt.get();
            command.setStatus(result.isSuccess() ? "completed" : "failed");
            command.setResult(result);
            commandRepository.save(command);
            
            logger.info("Command result saved for command: {}, success: {}", 
                       result.getCommandId(), result.isSuccess());
        }
    }
    
    public Long queueCommand(String containerId, CommandRequest commandRequest) {
        CommandQueue command = new CommandQueue();
        command.setContainerId(containerId);
        command.setCommandType(commandRequest.getType());
        command.setParameters(commandRequest.getParameters());
        command.setStatus("pending");
        command.setCreatedAt(LocalDateTime.now());
        
        CommandQueue savedCommand = commandRepository.save(command);
        
        logger.info("Command queued: {} for container: {}", savedCommand.getId(), containerId);
        return savedCommand.getId();
    }
    
    // Hierarchy navigation methods
    public List<String> getAllTeams() {
        return containerRepository.findDistinctTeamNames();
    }
    
    public List<String> getAppsByTeam(String teamName) {
        return containerRepository.findDistinctAppNamesByTeam(teamName);
    }
    
    public List<String> getPodsByTeamAndApp(String teamName, String appName) {
        return containerRepository.findDistinctPodNamesByTeamAndApp(teamName, appName);
    }
    
    public List<ContainerInfo> getContainersByPod(String teamName, String appName, String podName) {
        return containerRepository.findContainerInfoByPod(teamName, appName, podName);
    }
    
    public ContainerStatus getContainerStatus(String containerId) {
        return containerRepository.findContainerStatusById(containerId);
    }
    
    public CommandStatus getCommandStatus(Long commandId) {
        return commandRepository.findCommandStatusById(commandId);
    }
    
    public List<CommandHistory> getCommandHistory(String containerId, int page, int size) {
        return commandRepository.findCommandHistoryByContainerId(containerId, page, size);
    }
}