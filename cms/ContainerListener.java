// ContainerListener.java - Main listener service
package com.monitoring.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ContainerListener {
    private static final Logger logger = LoggerFactory.getLogger(ContainerListener.class);
    
    private final String containerId;
    private final String podName;
    private final String appName;
    private final String teamName;
    private final String centralApiUrl;
    private final ApiClient apiClient;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final String dumpDirectory;
    
    public ContainerListener() {
        this.teamName = getEnvOrDefault("TEAM_NAME", "unknown");
        this.appName = getEnvOrDefault("APP_NAME", "unknown");
        this.podName = getEnvOrDefault("POD_NAME", getHostname());
        this.containerId = generateContainerId();
        this.centralApiUrl = getEnvOrDefault("CENTRAL_API_URL", "http://monitoring-api:8080");
        this.dumpDirectory = getEnvOrDefault("DUMP_DIRECTORY", "/tmp/dumps");
        this.apiClient = new ApiClient(centralApiUrl);
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newScheduledThreadPool(3);
        
        // Ensure dump directory exists
        createDumpDirectory();
    }
    
    public void start() {
        logger.info("Starting Container Listener for container: {}", containerId);
        
        try {
            // Register container on startup
            registerContainer();
            
            // Start heartbeat
            startHeartbeat();
            
            // Start command polling
            startCommandPolling();
            
            logger.info("Container Listener started successfully");
            
        } catch (Exception e) {
            logger.error("Failed to start Container Listener", e);
            throw new RuntimeException("Container Listener startup failed", e);
        }
    }
    
    public void stop() {
        logger.info("Stopping Container Listener");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private void registerContainer() throws Exception {
        ContainerRegistration registration = new ContainerRegistration();
        registration.setContainerId(containerId);
        registration.setTeamName(teamName);
        registration.setAppName(appName);
        registration.setPodName(podName);
        registration.setContainerName(getEnvOrDefault("CONTAINER_NAME", "main"));
        registration.setHostIp(getHostIp());
        registration.setStatus("active");
        registration.setJvmInfo(getJvmInfo());
        
        boolean registered = apiClient.registerContainer(registration);
        if (!registered) {
            throw new RuntimeException("Failed to register container with central API");
        }
        
        logger.info("Container registered successfully: {}", containerId);
    }
    
    private void startHeartbeat() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                apiClient.sendHeartbeat(containerId);
                logger.debug("Heartbeat sent for container: {}", containerId);
            } catch (Exception e) {
                logger.warn("Failed to send heartbeat", e);
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    private void startCommandPolling() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Command command = apiClient.pollForCommand(containerId);
                if (command != null) {
                    handleCommand(command);
                }
            } catch (Exception e) {
                logger.warn("Error during command polling", e);
            }
        }, 5, 5, TimeUnit.SECONDS);
    }
    
    private void handleCommand(Command command) {
        logger.info("Received command: {} for container: {}", command.getType(), containerId);
        
        try {
            CommandResult result = new CommandResult();
            result.setCommandId(command.getId());
            result.setContainerId(containerId);
            result.setStartTime(LocalDateTime.now());
            
            switch (command.getType()) {
                case "HEAP_DUMP":
                    result = createHeapDump(command, result);
                    break;
                case "THREAD_DUMP":
                    result = createThreadDump(command, result);
                    break;
                case "GC_RUN":
                    result = runGarbageCollection(command, result);
                    break;
                case "SYSTEM_INFO":
                    result = getSystemInfo(command, result);
                    break;
                default:
                    result.setSuccess(false);
                    result.setErrorMessage("Unknown command type: " + command.getType());
            }
            
            result.setEndTime(LocalDateTime.now());
            apiClient.sendCommandResult(result);
            
        } catch (Exception e) {
            logger.error("Error executing command: " + command.getType(), e);
            CommandResult errorResult = new CommandResult();
            errorResult.setCommandId(command.getId());
            errorResult.setContainerId(containerId);
            errorResult.setSuccess(false);
            errorResult.setErrorMessage(e.getMessage());
            errorResult.setEndTime(LocalDateTime.now());
            
            try {
                apiClient.sendCommandResult(errorResult);
            } catch (Exception ex) {
                logger.error("Failed to send error result", ex);
            }
        }
    }
    
    private CommandResult createHeapDump(Command command, CommandResult result) throws Exception {
        String fileName = String.format("heapdump_%s_%s.hprof", containerId, System.currentTimeMillis());
        Path filePath = Paths.get(dumpDirectory, fileName);
        
        // Execute heap dump using JVM diagnostic commands
        String dumpCommand = String.format("jcmd %d GC.run_finalization", getCurrentPid());
        Process process = Runtime.getRuntime().exec(dumpCommand);
        process.waitFor();
        
        // Create heap dump
        String heapDumpCommand = String.format("jcmd %d GC.class_histogram > %s", getCurrentPid(), filePath.toString());
        Process heapProcess = Runtime.getRuntime().exec(heapDumpCommand);
        int exitCode = heapProcess.waitFor();
        
        if (exitCode == 0 && Files.exists(filePath)) {
            // Upload file to central API
            String fileId = apiClient.uploadFile(filePath.toFile(), "heap_dump");
            
            result.setSuccess(true);
            result.setMessage("Heap dump created successfully");
            result.addProperty("fileId", fileId);
            result.addProperty("fileName", fileName);
            result.addProperty("fileSize", Files.size(filePath));
            
            // Clean up local file after upload
            Files.deleteIfExists(filePath);
            
        } else {
            result.setSuccess(false);
            result.setErrorMessage("Failed to create heap dump");
        }
        
        return result;
    }
    
    private CommandResult createThreadDump(Command command, CommandResult result) throws Exception {
        ThreadMXBean threadMX = ManagementFactory.getThreadMXBean();
        StringBuilder threadDump = new StringBuilder();
        
        threadDump.append("Thread Dump - ").append(LocalDateTime.now()).append("\n");
        threadDump.append("=".repeat(50)).append("\n\n");
        
        long[] threadIds = threadMX.getAllThreadIds();
        for (long threadId : threadIds) {
            threadDump.append(threadMX.getThreadInfo(threadId, Integer.MAX_VALUE));
            threadDump.append("\n");
        }
        
        String fileName = String.format("threaddump_%s_%s.txt", containerId, System.currentTimeMillis());
        Path filePath = Paths.get(dumpDirectory, fileName);
        
        Files.write(filePath, threadDump.toString().getBytes());
        
        // Upload file to central API
        String fileId = apiClient.uploadFile(filePath.toFile(), "thread_dump");
        
        result.setSuccess(true);
        result.setMessage("Thread dump created successfully");
        result.addProperty("fileId", fileId);
        result.addProperty("fileName", fileName);
        result.addProperty("fileSize", Files.size(filePath));
        
        // Clean up local file after upload
        Files.deleteIfExists(filePath);
        
        return result;
    }
    
    private CommandResult runGarbageCollection(Command command, CommandResult result) throws Exception {
        long beforeMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        System.gc();
        Thread.sleep(1000); // Give GC time to complete
        
        long afterMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long freedMemory = beforeMemory - afterMemory;
        
        result.setSuccess(true);
        result.setMessage("Garbage collection completed");
        result.addProperty("memoryBeforeGC", beforeMemory);
        result.addProperty("memoryAfterGC", afterMemory);
        result.addProperty("memoryFreed", freedMemory);
        
        return result;
    }
    
    private CommandResult getSystemInfo(Command command, CommandResult result) throws Exception {
        Runtime runtime = Runtime.getRuntime();
        
        result.setSuccess(true);
        result.setMessage("System information retrieved");
        result.addProperty("availableProcessors", runtime.availableProcessors());
        result.addProperty("freeMemory", runtime.freeMemory());
        result.addProperty("totalMemory", runtime.totalMemory());
        result.addProperty("maxMemory", runtime.maxMemory());
        result.addProperty("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        result.addProperty("javaVersion", System.getProperty("java.version"));
        result.addProperty("osName", System.getProperty("os.name"));
        result.addProperty("osVersion", System.getProperty("os.version"));
        
        return result;
    }
    
    private JvmInfo getJvmInfo() {
        Runtime runtime = Runtime.getRuntime();
        JvmInfo info = new JvmInfo();
        info.setJavaVersion(System.getProperty("java.version"));
        info.setJvmName(System.getProperty("java.vm.name"));
        info.setMaxMemory(runtime.maxMemory());
        info.setAvailableProcessors(runtime.availableProcessors());
        return info;
    }
    
    private void createDumpDirectory() {
        try {
            Path path = Paths.get(dumpDirectory);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            logger.warn("Failed to create dump directory: {}", dumpDirectory, e);
        }
    }
    
    private String generateContainerId() {
        return String.format("%s_%s_%s_%d", teamName, appName, podName, System.currentTimeMillis());
    }
    
    private String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private String getHostIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private long getCurrentPid() {
        return ProcessHandle.current().pid();
    }
    
    private String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null && !value.trim().isEmpty() ? value : defaultValue;
    }
    
    // Main method to start listener as background service
    public static void main(String[] args) {
        ContainerListener listener = new ContainerListener();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(listener::stop));
        
        // Start listener
        listener.start();
        
        // Keep main thread alive
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}