# 🔧 .NET Application Instrumentation Guide for Linux

**OpenTelemetry & New Relic Auto-Instrumentation**

---

## Purpose

This guide helps you troubleshoot and configure low-code/no-code APM instrumentation for .NET applications running on Linux using OpenTelemetry or New Relic agents.

---

## 📋 Table of Contents

1. [OpenTelemetry Auto-Instrumentation](#1️⃣-opentelemetry-auto-instrumentation)
2. [New Relic Agent Setup](#2️⃣-new-relic-agent-setup)
3. [Common Issues & Solutions](#3️⃣-common-issues--solutions)
4. [Verification & Troubleshooting](#4️⃣-verification--troubleshooting)
5. [Container-Specific Configuration](#5️⃣-container-specific-configuration)
6. [Additional Resources](#-additional-resources)

---

## 1️⃣ OpenTelemetry Auto-Instrumentation

### Quick Setup (Recommended)

```bash
# Download and install the auto-instrumentation
curl -sSfL https://github.com/open-telemetry/opentelemetry-dotnet-instrumentation/releases/latest/download/otel-dotnet-auto-install.sh -O
sh ./otel-dotnet-auto-install.sh

# Source the instrumentation environment
. $HOME/.otel-dotnet-auto/instrument.sh

# Run your application
OTEL_SERVICE_NAME=myapp ./MyNetApp
```

### Required Environment Variables

```bash
# Essential CLR profiling variables
CORECLR_ENABLE_PROFILING=1
CORECLR_PROFILER={918728DD-259F-4A6A-AC2B-B85E1B658318}
CORECLR_PROFILER_PATH=/path/to/OpenTelemetry.AutoInstrumentation.Native.so

# .NET specific paths
DOTNET_ADDITIONAL_DEPS=/path/to/AdditionalDeps
DOTNET_SHARED_STORE=/path/to/store
DOTNET_STARTUP_HOOKS=/path/to/OpenTelemetry.AutoInstrumentation.StartupHook.dll

# OpenTelemetry home
OTEL_DOTNET_AUTO_HOME=/path/to/otel-dotnet-auto

# Configuration
OTEL_SERVICE_NAME=your-app-name
OTEL_EXPORTER_OTLP_ENDPOINT=http://your-collector:4318
OTEL_RESOURCE_ATTRIBUTES=deployment.environment=production
OTEL_TRACES_EXPORTER=otlp
OTEL_METRICS_EXPORTER=otlp
OTEL_LOGS_EXPORTER=otlp
```

> **⚠️ Important:** Starting in .NET 8, if `DOTNET_EnableDiagnostics` is set to 0, profiling is disabled. Ensure it's set to 1 or not set at all.

### Manual Installation Steps

1. Download the release from GitHub for your architecture (linux-x64, linux-arm64, etc.)
2. Extract to a permanent location (e.g., `/opt/opentelemetry-dotnet-instrumentation`)
3. Set all required environment variables with absolute paths
4. Restart your application

---

## 2️⃣ New Relic Agent Setup

### Installation

```bash
# Download New Relic .NET agent for Linux
wget https://download.newrelic.com/dot_net_agent/latest_release/newrelic-dotnet-agent_[VERSION]_amd64.deb
sudo dpkg -i newrelic-dotnet-agent_[VERSION]_amd64.deb

# Or using tar.gz
wget https://download.newrelic.com/dot_net_agent/latest_release/newrelic-dotnet-agent_[VERSION]_amd64.tar.gz
sudo tar -xzf newrelic-dotnet-agent_[VERSION]_amd64.tar.gz -C /usr/local/
```

### Required Environment Variables

```bash
# Core profiling variables
CORECLR_ENABLE_PROFILING=1
CORECLR_PROFILER={36032161-FFC0-4B61-B559-F6C5D41BAE5A}
CORECLR_NEWRELIC_HOME=/usr/local/newrelic-dotnet-agent
CORECLR_PROFILER_PATH=/usr/local/newrelic-dotnet-agent/libNewRelicProfiler.so

# Configuration
NEW_RELIC_LICENSE_KEY=your_license_key_here
NEW_RELIC_APP_NAME=your_app_name

# Optional but recommended
NEW_RELIC_LOG_LEVEL=info
NEW_RELIC_LOG_DIRECTORY=/var/log/newrelic
```

### Configuration File

Edit `/usr/local/newrelic-dotnet-agent/newrelic.config`:

```xml
<configuration xmlns="urn:newrelic-config">
  <service licenseKey="YOUR_LICENSE_KEY" />
  <application>
    <name>Your App Name</name>
  </application>
  <log level="info" />
  <agentEnabled>true</agentEnabled>
</configuration>
```

> **💡 Tip:** You can override config file settings with environment variables. Environment variables take precedence.

---

## 3️⃣ Common Issues & Solutions

### Issue: No Metrics Being Emitted

**Symptom:** Application runs but no telemetry data appears in APM

#### Checklist:

- ☐ Verify all environment variables are set at process startup
- ☐ Check profiler GUID is correct (different for OTel vs New Relic)
- ☐ Ensure profiler binary (.so file) exists at specified path
- ☐ Confirm file permissions allow your app user to read agent files
- ☐ Verify license key/endpoint is correct and accessible
- ☐ Check if another profiler is already attached (only one allowed)

### GUID Reference Table

| Tool | CORECLR_PROFILER GUID |
|------|----------------------|
| OpenTelemetry | {918728DD-259F-4A6A-AC2B-B85E1B658318} |
| New Relic | {36032161-FFC0-4B61-B559-F6C5D41BAE5A} |

### Issue: Environment Variables Not Applied

**For systemd services:**

```bash
# Edit your service file: /etc/systemd/system/myapp.service
[Service]
Environment="CORECLR_ENABLE_PROFILING=1"
Environment="CORECLR_PROFILER={36032161-FFC0-4B61-B559-F6C5D41BAE5A}"
Environment="CORECLR_NEWRELIC_HOME=/usr/local/newrelic-dotnet-agent"
Environment="CORECLR_PROFILER_PATH=/usr/local/newrelic-dotnet-agent/libNewRelicProfiler.so"
Environment="NEW_RELIC_LICENSE_KEY=your_key"
Environment="NEW_RELIC_APP_NAME=your_app"

# Reload and restart
sudo systemctl daemon-reload
sudo systemctl restart myapp
```

**For shell scripts:**

```bash
#!/bin/bash
export CORECLR_ENABLE_PROFILING=1
export CORECLR_PROFILER={36032161-FFC0-4B61-B559-F6C5D41BAE5A}
export CORECLR_NEWRELIC_HOME=/usr/local/newrelic-dotnet-agent
export CORECLR_PROFILER_PATH=/usr/local/newrelic-dotnet-agent/libNewRelicProfiler.so
export NEW_RELIC_LICENSE_KEY=your_key
export NEW_RELIC_APP_NAME=your_app

dotnet YourApp.dll
```

### Issue: Permission Denied

```bash
# Fix permissions for New Relic agent
sudo chmod -R 755 /usr/local/newrelic-dotnet-agent
sudo chown -R your-app-user:your-app-group /usr/local/newrelic-dotnet-agent

# Create log directory with proper permissions
sudo mkdir -p /var/log/newrelic
sudo chown your-app-user:your-app-group /var/log/newrelic
sudo chmod 755 /var/log/newrelic
```

---

## 4️⃣ Verification & Troubleshooting

### Step 1: Verify Agent Files Exist

```bash
# For New Relic
ls -la /usr/local/newrelic-dotnet-agent/
ls -la /usr/local/newrelic-dotnet-agent/libNewRelicProfiler.so

# For OpenTelemetry
ls -la $OTEL_DOTNET_AUTO_HOME/
ls -la $CORECLR_PROFILER_PATH
```

### Step 2: Check Environment Variables

```bash
# Find your app's process ID
ps aux | grep dotnet

# View environment variables for that process (replace PID)
sudo cat /proc/PID/environ | tr '\0' '\n' | grep -E "CORECLR|NEW_RELIC|OTEL"
```

### Step 3: Verify Profiler Attachment

```bash
# Check if profiler library is loaded (replace PID)
sudo cat /proc/PID/maps | grep -E "NewRelicProfiler|OpenTelemetry"

# If nothing shows, the profiler didn't attach
```

### Step 4: Enable Debug Logging

**For New Relic:**

```bash
export NEW_RELIC_LOG_LEVEL=debug
export NEWRELIC_PROFILER_LOG_DIRECTORY=/tmp/newrelic-logs

# Then restart your app and check:
cat /tmp/newrelic-logs/NewRelic.Profiler.*.log
cat /usr/local/newrelic-dotnet-agent/logs/newrelic_agent*.log
```

**For OpenTelemetry:**

```bash
export OTEL_LOG_LEVEL=debug
export OTEL_DOTNET_AUTO_LOG_DIRECTORY=/tmp/otel-logs

# Check logs in app directory or specified log directory
cat otel-dotnet-auto-*.log
```

### Step 5: Network Connectivity

```bash
# Test New Relic endpoint
curl -v https://collector.newrelic.com

# Test OpenTelemetry collector
curl -v http://your-collector:4318/v1/traces

# Check firewall rules
sudo iptables -L -n | grep -E "4317|4318"
```

### Common Log Errors & Solutions

| Error Message | Solution |
|--------------|----------|
| Profiler unable to attach | Check CORECLR_ENABLE_PROFILING=1 and GUID is correct |
| License key invalid | Verify NEW_RELIC_LICENSE_KEY or check New Relic account |
| Failed to load profiler library | Check CORECLR_PROFILER_PATH points to correct .so file |
| Permission denied | Fix file permissions with chmod/chown |
| Connection refused | Verify endpoint URL and network connectivity |

---

## 5️⃣ Container-Specific Configuration

### Docker - New Relic

```dockerfile
FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS base
WORKDIR /app
EXPOSE 80

# Install New Relic agent
RUN apt-get update && apt-get install -y wget ca-certificates && \
    wget https://download.newrelic.com/dot_net_agent/latest_release/newrelic-dotnet-agent_10.0.0_amd64.deb && \
    dpkg -i newrelic-dotnet-agent_10.0.0_amd64.deb && \
    rm newrelic-dotnet-agent_10.0.0_amd64.deb

# Set environment variables
ENV CORECLR_ENABLE_PROFILING=1 \
    CORECLR_PROFILER={36032161-FFC0-4B61-B559-F6C5D41BAE5A} \
    CORECLR_NEWRELIC_HOME=/usr/local/newrelic-dotnet-agent \
    CORECLR_PROFILER_PATH=/usr/local/newrelic-dotnet-agent/libNewRelicProfiler.so \
    NEW_RELIC_LICENSE_KEY=your_license_key_here \
    NEW_RELIC_APP_NAME=your_app_name

COPY --from=build /app/publish .
ENTRYPOINT ["dotnet", "YourApp.dll"]
```

### Docker - OpenTelemetry

```dockerfile
FROM mcr.microsoft.com/dotnet/aspnet:8.0 AS base
WORKDIR /app
EXPOSE 80

# Install OpenTelemetry auto-instrumentation
ADD https://github.com/open-telemetry/opentelemetry-dotnet-instrumentation/releases/latest/download/otel-dotnet-auto-install.sh otel-dotnet-auto-install.sh
RUN sh otel-dotnet-auto-install.sh && rm otel-dotnet-auto-install.sh

# Set environment variables
ENV CORECLR_ENABLE_PROFILING=1 \
    CORECLR_PROFILER={918728DD-259F-4A6A-AC2B-B85E1B658318} \
    CORECLR_PROFILER_PATH=/otel-dotnet-auto/linux-x64/OpenTelemetry.AutoInstrumentation.Native.so \
    DOTNET_STARTUP_HOOKS=/otel-dotnet-auto/net/OpenTelemetry.AutoInstrumentation.StartupHook.dll \
    DOTNET_ADDITIONAL_DEPS=/otel-dotnet-auto/AdditionalDeps \
    DOTNET_SHARED_STORE=/otel-dotnet-auto/store \
    OTEL_DOTNET_AUTO_HOME=/otel-dotnet-auto \
    OTEL_SERVICE_NAME=your-app-name \
    OTEL_EXPORTER_OTLP_ENDPOINT=http://collector:4318

COPY --from=build /app/publish .
ENTRYPOINT ["dotnet", "YourApp.dll"]
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: dotnet-app
spec:
  replicas: 1
  template:
    spec:
      containers:
      - name: app
        image: your-dotnet-app:latest
        env:
        - name: CORECLR_ENABLE_PROFILING
          value: "1"
        - name: CORECLR_PROFILER
          value: "{36032161-FFC0-4B61-B559-F6C5D41BAE5A}"
        - name: CORECLR_NEWRELIC_HOME
          value: "/usr/local/newrelic-dotnet-agent"
        - name: CORECLR_PROFILER_PATH
          value: "/usr/local/newrelic-dotnet-agent/libNewRelicProfiler.so"
        - name: NEW_RELIC_LICENSE_KEY
          valueFrom:
            secretKeyRef:
              name: newrelic-secret
              key: license-key
        - name: NEW_RELIC_APP_NAME
          value: "your-app-name"
```

> **⚠️ Security Note:** Always use Kubernetes secrets for sensitive data like license keys, never hardcode them in deployment manifests.

---

## 📚 Additional Resources

- [OpenTelemetry .NET Instrumentation](https://github.com/open-telemetry/opentelemetry-dotnet-instrumentation)
- [New Relic .NET Agent Docs](https://docs.newrelic.com/docs/apm/agents/net-agent/)
- [OTEL Collector Configuration](https://opentelemetry.io/docs/collector/)

> **💡 Pro Tip:** Start with debug logging enabled, verify the agent is working, then reduce logging to info or warn in production to minimize overhead.

---

*Generated guide for troubleshooting .NET APM instrumentation on Linux*  
*Last updated: December 2024*