# Vault-OpenShift Secret Sync Microservice

A production-ready microservice that synchronizes secrets from HashiCorp Vault to OpenShift secrets using FastAPI.

## Features

- **Secure Authentication**: Uses Vault AppRole authentication flow
- **Configurable Mappings**: External ConfigMap defines which secrets to sync
- **On-Demand Refresh**: REST API endpoint triggers secret synchronization
- **Health Monitoring**: Built-in health checks for Vault and OpenShift connectivity
- **Production Ready**: Proper RBAC, security contexts, and resource limits
- **Generic Design**: Same image can be used across environments with different configs

## Architecture Flow

1. **Authentication Flow**:
   ```
   Role ID + Secret ID → Vault Token → Access Secrets
   ```

2. **Synchronization Flow**:
   ```
   POST /api/refreshsecret → Fetch from Vault → Update OpenShift Secrets
   ```

## Quick Start

### 1. Build and Deploy

```bash
# Build the Docker image
docker build -t vault-sync-service:latest .

# Push to your registry
docker tag vault-sync-service:latest your-registry/vault-sync-service:latest
docker push your-registry/vault-sync-service:latest
```

### 2. Configure Vault

Set up AppRole authentication in Vault:

```bash
# Enable AppRole
vault auth enable approle

# Create a policy
vault policy write vault-sync-policy - <<EOF
path "secret/data/*" {
  capabilities = ["read"]
}
EOF

# Create AppRole
vault write auth/approle/role/vault-sync \
    token_policies="vault-sync-policy" \
    token_ttl=1h \
    token_max_ttl=4h

# Get Role ID and Secret ID
vault read auth/approle/role/vault-sync/role-id
vault write -f auth/approle/role/vault-sync/secret-id
```

### 3. Update Kubernetes Manifests

Update the `kubernetes-manifests.yaml`:

1. **Update Secret**: Replace base64-encoded credentials:
   ```bash
   echo -n "your-role-id" | base64
   echo -n "your-secret-id" | base64
   ```

2. **Update Vault Address**: Change `VAULT_ADDRESS` in deployment

3. **Update Secret Mappings**: Modify the ConfigMap with your secret paths

### 4. Deploy to OpenShift

```bash
# Create namespace
oc create namespace vault-sync

# Apply manifests
oc apply -f kubernetes-manifests.yaml

# Verify deployment
oc get pods -n vault-sync
oc logs -f deployment/vault-sync-service -n vault-sync
```

## API Endpoints

### Health Check
```bash
curl http://vault-sync-service/health
```

Response:
```json
{
  "status": "healthy",
  "timestamp": "2025-09-23T10:30:00Z",
  "vault_connectivity": true,
  "openshift_connectivity": true
}
```

### Refresh Secrets (Main Endpoint)
```bash
curl -X POST http://vault-sync-service/api/refreshsecret \
  -H "Content-Type: application/json" \
  -d '{}'
```

With custom namespace and mappings:
```bash
curl -X POST http://vault-sync-service/api/refreshsecret \
  -H "Content-Type: application/json" \
  -d '{
    "namespace": "production",
    "secrets": [
      {
        "vault_path": "secret/app/db",
        "vault_key": "password",
        "openshift_secret": "app-db-credentials",
        "openshift_key": "password"
      }
    ]
  }'
```

Response:
```json
{
  "status": "success",
  "message": "Successfully updated 2 secrets",
  "updated_secrets": ["app1-db-secret/db-password", "app1-api-secret/api-key"],
  "timestamp": "2025-09-23T10:30:00Z"
}
```

### Get Configuration
```bash
curl http://vault-sync-service/api/config
```

## Configuration

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `VAULT_ADDRESS` | Yes | - | Vault server URL |
| `VAULT_ROLE_ID` | Yes | - | Vault AppRole Role ID |
| `VAULT_SECRET_ID` | Yes | - | Vault AppRole Secret ID |
| `VAULT_MOUNT_PATH` | No | `secret` | Vault secrets mount path |
| `OPENSHIFT_NAMESPACE` | No | `default` | Default OpenShift namespace |
| `SECRET_MAPPINGS_FILE` | No | `/app/config/secret-mappings.yaml` | Path to secret mappings |
| `HTTP_TIMEOUT` | No | `30` | HTTP request timeout |
| `MAX_RETRIES` | No | `3` | Max retry attempts |
| `PORT` | No | `8000` | Service port |

### Secret Mappings Format

```yaml
mappings:
  - vault_path: "secret/app1/database"    # Path in Vault (without /data/)
    vault_key: "password"                 # Key within the secret
    openshift_secret: "app1-db-secret"    # Target OpenShift secret name
    openshift_key: "db-password"          # Target key in OpenShift secret
```

## Security Features

- **RBAC**: Minimal required permissions for secret management
- **Non-root execution**: Runs as non-root user (UID 1000)
- **Read-only filesystem**: Container filesystem is read-only
- **Security contexts**: Drops all capabilities, prevents privilege escalation
- **Secret management**: Sensitive data stored in Kubernetes secrets
- **Network policies**: Can be restricted using Kubernetes NetworkPolicies

## Monitoring and Observability

### Health Checks
- **Liveness**: `/health` endpoint with 30s interval
- **Readiness**: `/health` endpoint with 10s interval
- **Startup**: 30s initial delay for application startup

### Logging
- Structured JSON logging
- Different log levels: INFO, WARNING, ERROR
- Correlation IDs for request tracking

### Metrics (Future Enhancement)
The service can be extended with Prometheus metrics:
- Request duration
- Success/failure rates
- Vault token refresh frequency
- Secret update counts

## Multi-Environment Usage

The same image can be deployed across environments with different configurations:

### Development Environment
```yaml
env:
- name: VAULT_ADDRESS
  value: "https://vault-dev.example.com"
- name: OPENSHIFT_NAMESPACE
  value: "development"
```

### Production Environment
```yaml
env:
- name: VAULT_ADDRESS
  value: "https://vault-prod.example.com"
- name: OPENSHIFT_NAMESPACE
  value: "production"
```

## Automation and CI/CD Integration

### GitOps Integration
```bash
# Trigger refresh after deployment
curl -X POST https://vault-sync.apps.cluster.com/api/refreshsecret
```

### Scheduled Refresh (CronJob)
```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: vault-sync-scheduler
  namespace: vault-sync
spec:
  schedule: "0 */6 * * *"  # Every 6 hours
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: curl
            image: curlimages/curl:latest
            command:
            - /bin/sh
            - -c
            - |
              curl -X POST http://vault-sync-service.vault-sync.svc.cluster.local/api/refreshsecret \
                -H "Content-Type: application/json" \
                -d '{}' || exit 1
          restartPolicy: OnFailure
      backoffLimit: 3
```

## Troubleshooting

### Common Issues

#### 1. Vault Authentication Failures
```bash
# Check vault connectivity
oc exec -it deployment/vault-sync-service -- curl -k https://vault.example.com/v1/sys/health

# Verify credentials
oc get secret vault-credentials -o yaml
echo "base64-string" | base64 -d  # Decode to verify

# Check logs for authentication errors
oc logs deployment/vault-sync-service | grep -i vault
```

#### 2. OpenShift Permission Issues
```bash
# Verify service account permissions
oc auth can-i create secrets --as=system:serviceaccount:vault-sync:vault-sync-sa

# Check RBAC bindings
oc describe clusterrolebinding vault-sync-secrets-binding

# Test secret creation manually
oc create secret generic test-secret --from-literal=key=value -n target-namespace
```

#### 3. Configuration Issues
```bash
# Check ConfigMap
oc get configmap vault-sync-config -o yaml

# Validate secret mappings
oc exec -it deployment/vault-sync-service -- cat /app/config/secret-mappings.yaml

# Check environment variables
oc set env deployment/vault-sync-service --list
```

### Debug Mode
Enable debug logging by adding to deployment:
```yaml
env:
- name: LOG_LEVEL
  value: "DEBUG"
```

### Testing Secret Mappings
```bash
# Test specific vault path
vault kv get secret/app1/database

# Check if target secret exists
oc get secret app1-db-secret -o yaml

# Manual API call with curl from pod
oc exec -it deployment/vault-sync-service -- curl -X POST localhost:8000/api/refreshsecret -H "Content-Type: application/json" -d '{}'
```

## Advanced Configuration

### Custom Vault Authentication
For different auth methods, extend the `VaultClient` class:

```python
# For Kubernetes auth
async def k8s_auth(self, role: str, jwt_token: str) -> str:
    url = f"{self.base_url}/v1/auth/kubernetes/login"
    payload = {"role": role, "jwt": jwt_token}
    response = await self.session.post(url, json=payload)
    data = response.json()
    return data["auth"]["client_token"]
```

### Multiple Vault Instances
Deploy multiple instances with different configurations:

```yaml
# Production Vault instance
apiVersion: apps/v1
kind: Deployment
metadata:
  name: vault-sync-prod
spec:
  template:
    spec:
      containers:
      - name: vault-sync
        env:
        - name: VAULT_ADDRESS
          value: "https://vault-prod.example.com"
        - name: SECRET_MAPPINGS_FILE
          value: "/app/config/prod-mappings.yaml"

---
# Development Vault instance  
apiVersion: apps/v1
kind: Deployment
metadata:
  name: vault-sync-dev
spec:
  template:
    spec:
      containers:
      - name: vault-sync
        env:
        - name: VAULT_ADDRESS
          value: "https://vault-dev.example.com"
        - name: SECRET_MAPPINGS_FILE
          value: "/app/config/dev-mappings.yaml"
```

### Network Policies
Restrict network access:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: vault-sync-netpol
  namespace: vault-sync
spec:
  podSelector:
    matchLabels:
      app: vault-sync-service
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: monitoring
    ports:
    - protocol: TCP
      port: 8000
  egress:
  - to: []  # Vault and K8s API
    ports:
    - protocol: TCP
      port: 443
    - protocol: TCP
      port: 6443
  - to: []  # DNS
    ports:
    - protocol: UDP
      port: 53
```

## Performance Tuning

### Resource Optimization
```yaml
resources:
  requests:
    memory: "64Mi"    # Minimum for small deployments
    cpu: "50m"
  limits:
    memory: "512Mi"   # Increase for high-volume environments
    cpu: "500m"
```

### Concurrent Processing
Modify the service to handle multiple namespaces concurrently:

```python
import asyncio

async def refresh_multiple_namespaces(namespaces: List[str]):
    tasks = [refresh_secrets_task(ns, config_instance.secret_mappings) 
             for ns in namespaces]
    results = await asyncio.gather(*tasks, return_exceptions=True)
    return results
```

### Caching
Implement token caching with TTL:

```python
from datetime import datetime, timedelta

class TokenCache:
    def __init__(self):
        self.token = None
        self.expires_at = None
    
    def is_valid(self):
        return self.token and datetime.utcnow() < self.expires_at
    
    def set_token(self, token: str, ttl_seconds: int = 3600):
        self.token = token
        self.expires_at = datetime.utcnow() + timedelta(seconds=ttl_seconds)
```

## Security Hardening

### Pod Security Standards
```yaml
apiVersion: v1
kind: Pod
metadata:
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted
spec:
  securityContext:
    runAsNonRoot: true
    runAsUser: 1000
    runAsGroup: 1000
    fsGroup: 1000
    seccompProfile:
      type: RuntimeDefault
  containers:
  - name: vault-sync
    securityContext:
      allowPrivilegeEscalation: false
      readOnlyRootFilesystem: true
      runAsNonRoot: true
      runAsUser: 1000
      capabilities:
        drop:
        - ALL
```

### Secret Rotation
Implement automatic secret rotation:

```bash
# Create rotation script
#!/bin/bash
NEW_SECRET_ID=$(vault write -f auth/approle/role/vault-sync/secret-id | grep secret_id | awk '{print $2}')
kubectl patch secret vault-credentials -n vault-sync \
  --patch="{\"data\":{\"vault-secret-id\":\"$(echo -n $NEW_SECRET_ID | base64)\"}}"
kubectl rollout restart deployment/vault-sync-service -n vault-sync
```

## Monitoring Integration

### Prometheus Metrics
Add metrics endpoint:

```python
from prometheus_client import Counter, Histogram, generate_latest
from fastapi import Response

# Metrics
request_count = Counter('vault_sync_requests_total', 'Total requests', ['method', 'endpoint'])
request_duration = Histogram('vault_sync_request_duration_seconds', 'Request duration')
secret_updates = Counter('vault_sync_secrets_updated_total', 'Secrets updated', ['namespace'])

@app.get("/metrics")
async def metrics():
    return Response(generate_latest(), media_type="text/plain")
```

### Grafana Dashboard
Create dashboard queries:
```promql
# Success rate
rate(vault_sync_requests_total{status="success"}[5m]) / rate(vault_sync_requests_total[5m]) * 100

# Average response time
rate(vault_sync_request_duration_seconds_sum[5m]) / rate(vault_sync_request_duration_seconds_count[5m])

# Secrets updated per hour
increase(vault_sync_secrets_updated_total[1h])
```

## Integration Examples

### Tekton Pipeline
```yaml
apiVersion: tekton.dev/v1beta1
kind: Task
metadata:
  name: refresh-vault-secrets
spec:
  steps:
  - name: refresh-secrets
    image: curlimages/curl:latest
    script: |
      #!/bin/sh
      curl -X POST $(params.vault-sync-url)/api/refreshsecret \
        -H "Content-Type: application/json" \
        -d '{"namespace": "$(params.target-namespace)"}' \
        --fail-with-body
  params:
  - name: vault-sync-url
    default: "http://vault-sync-service.vault-sync.svc.cluster.local"
  - name: target-namespace
    default: "default"
```

### ArgoCD PostSync Hook
```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: refresh-secrets-postsync
  annotations:
    argocd.argoproj.io/hook: PostSync
    argocd.argoproj.io/hook-delete-policy: BeforeHookCreation
spec:
  template:
    spec:
      containers:
      - name: refresh-secrets
        image: curlimages/curl:latest
        command:
        - sh
        - -c
        - |
          curl -X POST http://vault-sync-service.vault-sync.svc.cluster.local/api/refreshsecret \
            -H "Content-Type: application/json" \
            -d '{"namespace": "'$ARGOCD_APP_NAMESPACE'"}' \
            --fail-with-body
      restartPolicy: Never
```

This comprehensive microservice provides a robust, secure, and scalable solution for synchronizing secrets from Vault to OpenShift, with extensive configuration options and production-ready features.