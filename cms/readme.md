# Java Container Monitoring System for AKS

## Architecture Overview

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Web GUI       │    │  Central API     │    │   Storage       │
│                 │────│                  │────│                 │
│ - Team Selection│    │ - Registration   │    │ - PostgreSQL    │
│ - App Selection │    │ - Command Queue  │    │ - File Storage  │
│ - Pod Drilling  │    │ - Response Mgmt  │    │ - Redis Cache   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                                │
                                │ REST API / WebSocket
                                │
                    ┌───────────┼───────────┐
                    │           │           │
            ┌───────▼───┐  ┌────▼────┐  ┌──▼──────┐
            │  Pod 1    │  │  Pod 2  │  │  Pod N  │
            │           │  │         │  │         │
            │ Listener  │  │Listener │  │Listener │
            │ Agent     │  │ Agent   │  │ Agent   │
            └───────────┘  └─────────┘  └─────────┘
```

## Component Details

### 1. Container Listener Agent (Java)

**Purpose**: Background service in each container that registers and listens for commands

**Key Features**:
- Auto-registration on startup
- Command polling/websocket listening
- JVM diagnostics execution
- Response handling

### 2. Central API Service

**Purpose**: Orchestrates communication between GUI and containers

**Key Features**:
- Container registry management
- Command queuing system
- File upload/download handling
- WebSocket connections for real-time communication

### 3. Web GUI

**Purpose**: User interface for managing and monitoring containers

**Key Features**:
- Hierarchical selection (Team → App → Pod → Container)
- Command execution interface
- Real-time status updates
- File download capabilities

## Implementation Components

### Container Listener Agent

```java
// Main listener service that runs as background thread
public class ContainerListener {
    private String containerId;
    private String podName;
    private String appName;
    private String teamName;
    private ApiClient apiClient;
    
    public void start() {
        // Register container
        // Start command polling
        // Handle JVM operations
    }
}
```

### Central API Endpoints

```
POST   /api/containers/register     - Container registration
GET    /api/teams                   - List teams
GET    /api/teams/{team}/apps       - List apps for team  
GET    /api/apps/{app}/pods         - List pods for app
GET    /api/pods/{pod}/containers   - List containers for pod
POST   /api/containers/{id}/command - Send command to container
GET    /api/containers/{id}/status  - Get container status
GET    /api/files/{fileId}          - Download generated files
```

### Database Schema

```sql
-- Container registry
CREATE TABLE containers (
    id VARCHAR(255) PRIMARY KEY,
    team_name VARCHAR(100) NOT NULL,
    app_name VARCHAR(100) NOT NULL,
    pod_name VARCHAR(100) NOT NULL,
    container_name VARCHAR(100) NOT NULL,
    host_ip VARCHAR(50),
    status VARCHAR(20) DEFAULT 'active',
    last_heartbeat TIMESTAMP,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Command queue
CREATE TABLE command_queue (
    id SERIAL PRIMARY KEY,
    container_id VARCHAR(255) REFERENCES containers(id),
    command_type VARCHAR(50) NOT NULL,
    parameters JSONB,
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    executed_at TIMESTAMP,
    result JSONB
);

-- File storage references
CREATE TABLE generated_files (
    id SERIAL PRIMARY KEY,
    container_id VARCHAR(255) REFERENCES containers(id),
    file_type VARCHAR(50) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);
```

## Deployment Configuration

### Dockerfile Modifications

```dockerfile
FROM openjdk:11-jre-slim

# Add listener agent
COPY listener-agent.jar /app/
COPY start-with-listener.sh /app/

# Environment variables for registration
ENV TEAM_NAME=""
ENV APP_NAME=""
ENV CENTRAL_API_URL=""

ENTRYPOINT ["/app/start-with-listener.sh"]
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: java-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: java-app
  template:
    metadata:
      labels:
        app: java-app
        team: backend-team
    spec:
      containers:
      - name: java-app
        image: your-app:latest
        env:
        - name: TEAM_NAME
          value: "backend-team"
        - name: APP_NAME
          value: "user-service"
        - name: CENTRAL_API_URL
          value: "http://monitoring-api:8080"
        - name: POD_NAME
          valueFrom:
            fieldRef:
              fieldPath: metadata.name
        - name: CONTAINER_NAME
          value: "java-app"
        volumeMounts:
        - name: dumps-volume
          mountPath: /tmp/dumps
      volumes:
      - name: dumps-volume
        emptyDir: {}
```

## Security Considerations

### Authentication & Authorization
- API key authentication for container registration
- Role-based access control for GUI users
- Network policies to restrict API access
- Encrypted communication between components

### File Security
- Temporary file cleanup policies
- Size limits for heap/thread dumps
- Secure file download with expiration tokens

## Monitoring & Observability

### Health Checks
- Container heartbeat mechanism
- API health endpoints
- Database connection monitoring
- File storage health checks

### Logging Strategy
- Structured logging for all components
- Centralized log aggregation
- Audit trails for command execution
- Performance metrics collection

## Scalability Considerations

### High Availability
- Multiple API service instances
- Database clustering/replication
- Distributed file storage
- Load balancing for GUI

### Performance Optimization
- Connection pooling for database
- Caching for frequently accessed data
- Async command processing
- Batch operations for bulk actions

## Implementation Phases

### Phase 1: Core Infrastructure
1. Container listener agent
2. Basic API service
3. Simple registration system
4. Command execution framework

### Phase 2: Enhanced Features
1. Web GUI implementation
2. File handling system
3. Real-time updates
4. Advanced filtering

### Phase 3: Production Ready
1. Security implementation
2. Monitoring & alerting
3. High availability setup
4. Performance optimization

This architecture provides a robust foundation for managing and debugging Java containers in AKS while maintaining scalability and security.

Implementation Complete!
Your Java container monitoring system for AKS is now fully implemented with the following components:
🏗️ Architecture Components:

Container Listener Agent - Runs as background service in each Java container
Central API Service - Spring Boot application managing all communication
React GUI - Modern web interface for container management
Database Layer - PostgreSQL with proper schema and indexing
File Storage - Secure file handling for dumps and logs
Kubernetes Configuration - Complete deployment setup

🚀 Key Features Implemented:

Auto-registration of containers on startup
Hierarchical navigation (Team → App → Pod → Container)
Real-time command execution (heap dumps, thread dumps, GC, system info)
File upload/download for generated dumps
Heartbeat monitoring with automatic health checks
Command history and status tracking
Security with API keys and network policies
Scalability with HPA and load balancing
Monitoring with Prometheus integration

📝 Next Steps to Deploy:

Build Docker Images:
bash# Build listener agent JAR
# Build central API Docker image
# Build React GUI and package with nginx

Configure Environment:
bashkubectl apply -f monitoring-namespace.yaml
kubectl apply -f monitoring-secrets.yaml
kubectl apply -f monitoring-storage.yaml

Deploy Infrastructure:
bashkubectl apply -f postgres-deployment.yaml
kubectl apply -f redis-deployment.yaml
kubectl apply -f api-deployment.yaml
kubectl apply -f gui-deployment.yaml

Deploy Java Applications:
bash# Update your existing deployments to include:
# - Listener agent JAR
# - Environment variables
# - Startup script