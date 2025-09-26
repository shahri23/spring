#!/usr/bin/env python3
"""
Vault-OpenShift Secret Sync Microservice
A FastAPI microservice that fetches secrets from HashiCorp Vault and updates OpenShift secrets.
"""

import asyncio
import base64
import logging
import os
from datetime import datetime
from typing import Dict, List, Optional, Any

import httpx
from fastapi import FastAPI, HTTPException, BackgroundTasks, Depends
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
import yaml
from kubernetes import client, config
from kubernetes.client.rest import ApiException

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Pydantic models
class SecretMapping(BaseModel):
    vault_path: str = Field(..., description="Vault secret path")
    vault_key: str = Field(..., description="Key within the vault secret")
    openshift_secret: str = Field(..., description="OpenShift secret name")
    openshift_key: str = Field(..., description="Key within the OpenShift secret")

class RefreshRequest(BaseModel):
    namespace: Optional[str] = Field(None, description="Override namespace")
    secrets: Optional[List[SecretMapping]] = Field(None, description="Override secret mappings")

class RefreshResponse(BaseModel):
    status: str
    message: str
    updated_secrets: List[str]
    timestamp: datetime

class HealthResponse(BaseModel):
    status: str
    timestamp: datetime
    vault_connectivity: bool
    openshift_connectivity: bool

# Configuration class
class Config:
    def __init__(self):
        # Vault configuration
        self.vault_address = os.getenv("VAULT_ADDRESS")
        self.vault_role_id = os.getenv("VAULT_ROLE_ID")
        self.vault_secret_id = os.getenv("VAULT_SECRET_ID")
        self.vault_mount_path = os.getenv("VAULT_MOUNT_PATH", "secret")
        
        # OpenShift configuration
        self.openshift_namespace = os.getenv("OPENSHIFT_NAMESPACE", "default")
        
        # Secret mappings from configmap
        self.secret_mappings = self._load_secret_mappings()
        
        # HTTP client settings
        self.http_timeout = int(os.getenv("HTTP_TIMEOUT", "30"))
        self.max_retries = int(os.getenv("MAX_RETRIES", "3"))
        
        self._validate_config()
    
    def _load_secret_mappings(self) -> List[SecretMapping]:
        """Load secret mappings from configmap file or environment."""
        mappings_file = os.getenv("SECRET_MAPPINGS_FILE", "/app/config/secret-mappings.yaml")
        
        try:
            if os.path.exists(mappings_file):
                with open(mappings_file, 'r') as f:
                    data = yaml.safe_load(f)
                    return [SecretMapping(**mapping) for mapping in data.get('mappings', [])]
            else:
                logger.warning(f"Secret mappings file not found: {mappings_file}")
                return []
        except Exception as e:
            logger.error(f"Failed to load secret mappings: {e}")
            return []
    
    def _validate_config(self):
        """Validate required configuration."""
        required_vars = ["VAULT_ADDRESS", "VAULT_ROLE_ID", "VAULT_SECRET_ID"]
        missing_vars = [var for var in required_vars if not getattr(self, var.lower())]
        
        if missing_vars:
            raise ValueError(f"Missing required environment variables: {missing_vars}")

# Initialize configuration
config_instance = Config()

# FastAPI app initialization
app = FastAPI(
    title="Vault-OpenShift Secret Sync",
    description="Microservice for syncing secrets from Vault to OpenShift",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Global variables
vault_token: Optional[str] = None
k8s_client: Optional[client.CoreV1Api] = None

class VaultClient:
    def __init__(self, base_url: str, timeout: int = 30):
        self.base_url = base_url.rstrip('/')
        self.timeout = timeout
        self.session = None
    
    async def __aenter__(self):
        self.session = httpx.AsyncClient(timeout=self.timeout)
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if self.session:
            await self.session.aclose()
    
    async def get_secret_id(self, role_id: str) -> str:
        """Step 1: Get secret ID using role ID."""
        url = f"{self.base_url}/v1/auth/approle/role/{role_id}/secret-id"
        headers = {"Content-Type": "application/json"}
        
        response = await self.session.post(url, headers=headers)
        response.raise_for_status()
        
        data = response.json()
        return data["data"]["secret_id"]
    
    async def get_token(self, role_id: str, secret_id: str) -> str:
        """Step 2: Get token using role ID and secret ID."""
        url = f"{self.base_url}/v1/auth/approle/login"
        payload = {
            "role_id": role_id,
            "secret_id": secret_id
        }
        headers = {"Content-Type": "application/json"}
        
        response = await self.session.post(url, json=payload, headers=headers)
        response.raise_for_status()
        
        data = response.json()
        return data["auth"]["client_token"]
    
    async def get_secret(self, token: str, path: str) -> Dict[str, Any]:
        """Step 3: Get secret using token."""
        url = f"{self.base_url}/v1/{config_instance.vault_mount_path}/data/{path.lstrip('/')}"
        headers = {
            "X-Vault-Token": token,
            "Content-Type": "application/json"
        }
        
        response = await self.session.get(url, headers=headers)
        response.raise_for_status()
        
        data = response.json()
        return data["data"]["data"]

class OpenShiftClient:
    def __init__(self):
        self.api_client = None
        self._initialize_client()
    
    def _initialize_client(self):
        """Initialize Kubernetes client."""
        try:
            # Try to load in-cluster config first
            config.load_incluster_config()
            logger.info("Loaded in-cluster Kubernetes config")
        except config.ConfigException:
            try:
                # Fallback to local kubeconfig
                config.load_kube_config()
                logger.info("Loaded local Kubernetes config")
            except config.ConfigException as e:
                logger.error(f"Failed to load Kubernetes config: {e}")
                raise
        
        self.api_client = client.CoreV1Api()
    
    async def update_secret(self, namespace: str, secret_name: str, key: str, value: str):
        """Update or create a secret in OpenShift."""
        try:
            # Encode value to base64
            encoded_value = base64.b64encode(value.encode()).decode()
            
            # Try to get existing secret
            try:
                secret = self.api_client.read_namespaced_secret(
                    name=secret_name,
                    namespace=namespace
                )
                # Update existing secret
                if not secret.data:
                    secret.data = {}
                secret.data[key] = encoded_value
                
                self.api_client.patch_namespaced_secret(
                    name=secret_name,
                    namespace=namespace,
                    body=secret
                )
                logger.info(f"Updated secret {secret_name}/{key} in namespace {namespace}")
                
            except ApiException as e:
                if e.status == 404:
                    # Create new secret
                    new_secret = client.V1Secret(
                        metadata=client.V1ObjectMeta(
                            name=secret_name,
                            namespace=namespace
                        ),
                        data={key: encoded_value}
                    )
                    
                    self.api_client.create_namespaced_secret(
                        namespace=namespace,
                        body=new_secret
                    )
                    logger.info(f"Created secret {secret_name}/{key} in namespace {namespace}")
                else:
                    raise
                    
        except ApiException as e:
            logger.error(f"Failed to update secret {secret_name}/{key}: {e}")
            raise HTTPException(status_code=500, detail=f"Failed to update secret: {e}")

# Initialize OpenShift client
openshift_client = OpenShiftClient()

async def get_vault_token() -> str:
    """Get or refresh Vault token."""
    global vault_token
    
    if vault_token:
        return vault_token
    
    async with VaultClient(config_instance.vault_address, config_instance.http_timeout) as vault:
        try:
            # Step 1: Get secret ID (if needed - in this case we already have it)
            # Step 2: Get token
            vault_token = await vault.get_token(
                config_instance.vault_role_id,
                config_instance.vault_secret_id
            )
            logger.info("Successfully obtained Vault token")
            return vault_token
            
        except Exception as e:
            logger.error(f"Failed to obtain Vault token: {e}")
            raise HTTPException(status_code=500, detail=f"Vault authentication failed: {e}")

async def refresh_secrets_task(
    namespace: str,
    secret_mappings: List[SecretMapping]
) -> List[str]:
    """Background task to refresh secrets."""
    updated_secrets = []
    
    try:
        # Get Vault token
        token = await get_vault_token()
        
        async with VaultClient(config_instance.vault_address, config_instance.http_timeout) as vault:
            for mapping in secret_mappings:
                try:
                    # Fetch secret from Vault
                    secret_data = await vault.get_secret(token, mapping.vault_path)
                    
                    if mapping.vault_key not in secret_data:
                        logger.warning(f"Key {mapping.vault_key} not found in Vault secret {mapping.vault_path}")
                        continue
                    
                    secret_value = secret_data[mapping.vault_key]
                    
                    # Update OpenShift secret
                    await openshift_client.update_secret(
                        namespace=namespace,
                        secret_name=mapping.openshift_secret,
                        key=mapping.openshift_key,
                        value=secret_value
                    )
                    
                    updated_secrets.append(f"{mapping.openshift_secret}/{mapping.openshift_key}")
                    
                except Exception as e:
                    logger.error(f"Failed to sync secret {mapping.vault_path}: {e}")
                    continue
    
    except Exception as e:
        logger.error(f"Failed to refresh secrets: {e}")
        raise
    
    return updated_secrets

# API Routes
@app.get("/health", response_model=HealthResponse)
async def health_check():
    """Health check endpoint."""
    vault_ok = False
    openshift_ok = False
    
    # Check Vault connectivity
    try:
        async with VaultClient(config_instance.vault_address, 5) as vault:
            # Simple sys/health check
            response = await vault.session.get(f"{vault.base_url}/v1/sys/health")
            vault_ok = response.status_code in [200, 429, 472, 473]  # Vault health status codes
    except Exception:
        pass
    
    # Check OpenShift connectivity
    try:
        openshift_client.api_client.list_namespace(_request_timeout=5)
        openshift_ok = True
    except Exception:
        pass
    
    status = "healthy" if vault_ok and openshift_ok else "degraded"
    
    return HealthResponse(
        status=status,
        timestamp=datetime.utcnow(),
        vault_connectivity=vault_ok,
        openshift_connectivity=openshift_ok
    )

@app.post("/api/refreshsecret", response_model=RefreshResponse)
async def refresh_secrets(
    request: RefreshRequest,
    background_tasks: BackgroundTasks
):
    """Trigger secret refresh process."""
    namespace = request.namespace or config_instance.openshift_namespace
    mappings = request.secrets or config_instance.secret_mappings
    
    if not mappings:
        raise HTTPException(
            status_code=400,
            detail="No secret mappings configured. Provide mappings in request or configmap."
        )
    
    try:
        # Execute refresh synchronously for immediate response
        updated_secrets = await refresh_secrets_task(namespace, mappings)
        
        return RefreshResponse(
            status="success",
            message=f"Successfully updated {len(updated_secrets)} secrets",
            updated_secrets=updated_secrets,
            timestamp=datetime.utcnow()
        )
        
    except Exception as e:
        logger.error(f"Secret refresh failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/config")
async def get_config():
    """Get current configuration (sensitive data excluded)."""
    return {
        "vault_address": config_instance.vault_address,
        "vault_mount_path": config_instance.vault_mount_path,
        "openshift_namespace": config_instance.openshift_namespace,
        "secret_mappings_count": len(config_instance.secret_mappings),
        "http_timeout": config_instance.http_timeout,
        "max_retries": config_instance.max_retries
    }

@app.get("/")
async def root():
    """Root endpoint."""
    return {
        "service": "Vault-OpenShift Secret Sync",
        "version": "1.0.0",
        "status": "running",
        "endpoints": {
            "health": "/health",
            "refresh": "/api/refreshsecret",
            "config": "/api/config"
        }
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=int(os.getenv("PORT", "8000")),
        log_level="info"
    )