#!/bin/bash

# Create ASP.NET Core Hello World with New Relic on Alpine
echo "Creating project structure..."

# Create Dockerfile
cat > Dockerfile << 'EOF'
# Use Alpine-based ASP.NET runtime
FROM mcr.microsoft.com/dotnet/aspnet:8.0-alpine AS base
WORKDIR /app
EXPOSE 8080

# Build stage
FROM mcr.microsoft.com/dotnet/sdk:8.0-alpine AS build
WORKDIR /src

# Copy csproj and restore
COPY ["HelloWorld.csproj", "./"]
RUN dotnet restore "HelloWorld.csproj"

# Copy everything else and build
COPY . .
RUN dotnet build "HelloWorld.csproj" -c Release -o /app/build

# Publish stage
FROM build AS publish
RUN dotnet publish "HelloWorld.csproj" -c Release -o /app/publish /p:UseAppHost=false

# Final stage
FROM base AS final
WORKDIR /app
COPY --from=publish /app/publish .

# Set environment variables for New Relic (these will be overridden at runtime)
ENV CORECLR_ENABLE_PROFILING=1 \
    CORECLR_PROFILER={36032161-FFC0-4B61-B559-F6C5D41BAE5A} \
    CORECLR_NEWRELIC_HOME=/app/newrelic \
    CORECLR_PROFILER_PATH=/app/newrelic/libNewRelicProfiler.so \
    NEW_RELIC_LICENSE_KEY="" \
    NEW_RELIC_APP_NAME="HelloWorld-Alpine"

ENTRYPOINT ["dotnet", "HelloWorld.dll"]
EOF

# Create HelloWorld.csproj
cat > HelloWorld.csproj << 'EOF'
<Project Sdk="Microsoft.NET.Sdk.Web">

  <PropertyGroup>
    <TargetFramework>net8.0</TargetFramework>
    <Nullable>enable</Nullable>
    <ImplicitUsings>enable</ImplicitUsings>
  </PropertyGroup>

  <ItemGroup>
    <PackageReference Include="NewRelic.Agent" Version="10.29.0" />
  </ItemGroup>

</Project>
EOF

# Create Program.cs
cat > Program.cs << 'EOF'
var builder = WebApplication.CreateBuilder(args);

var app = builder.Build();

app.MapGet("/", () => "Hello World from ASP.NET Core with New Relic on Alpine!");

app.MapGet("/health", () => new { status = "healthy", timestamp = DateTime.UtcNow });

app.Run();
EOF

# Create docker-compose.yml
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  helloworld:
    build:
      context: .
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      # New Relic Configuration
      - NEW_RELIC_LICENSE_KEY=${NEW_RELIC_LICENSE_KEY}
      - NEW_RELIC_APP_NAME=${NEW_RELIC_APP_NAME:-HelloWorld-Alpine}
      
      # .NET Profiler Environment Variables for New Relic
      - CORECLR_ENABLE_PROFILING=1
      - CORECLR_PROFILER={36032161-FFC0-4B61-B559-F6C5D41BAE5A}
      - CORECLR_NEWRELIC_HOME=/app/newrelic
      - CORECLR_PROFILER_PATH=/app/newrelic/libNewRelicProfiler.so
      
      # Optional: New Relic additional settings
      - NEW_RELIC_LOG_LEVEL=${NEW_RELIC_LOG_LEVEL:-info}
      - NEW_RELIC_DISTRIBUTED_TRACING_ENABLED=true
      - ASPNETCORE_URLS=http://+:8080
EOF

# Create .env.example
cat > .env.example << 'EOF'
# Copy this file to .env and fill in your New Relic details

# Your New Relic License Key (REQUIRED)
NEW_RELIC_LICENSE_KEY=your_license_key_here

# Application name as it appears in New Relic
NEW_RELIC_APP_NAME=HelloWorld-Alpine

# Optional: Set log level (debug, info, warn, error)
NEW_RELIC_LOG_LEVEL=info
EOF

# Create README.md
cat > README.md << 'EOF'
# ASP.NET Core Hello World with New Relic on Alpine

A minimal ASP.NET Core 8.0 application with New Relic APM monitoring, running on Alpine Linux.

## Files Structure

```
.
├── Dockerfile
├── HelloWorld.csproj
├── Program.cs
├── docker-compose.yml
├── .env.example
└── README.md
```

## Prerequisites

- Docker and Docker Compose
- New Relic License Key

## Setup

1. **Create a `.env` file** from the example:
   ```bash
   cp .env.example .env
   ```

2. **Edit `.env`** and add your New Relic License Key:
   ```
   NEW_RELIC_LICENSE_KEY=your_actual_license_key
   NEW_RELIC_APP_NAME=HelloWorld-Alpine
   ```

## Build and Run

### Using Docker Compose (Recommended)

```bash
docker-compose up --build
```

### Using Docker directly

```bash
# Build the image
docker build -t helloworld-nr-alpine .

# Run with environment variables
docker run -p 8080:8080 \
  -e NEW_RELIC_LICENSE_KEY=your_license_key \
  -e NEW_RELIC_APP_NAME=HelloWorld-Alpine \
  -e CORECLR_ENABLE_PROFILING=1 \
  -e CORECLR_PROFILER={36032161-FFC0-4B61-B559-F6C5D41BAE5A} \
  -e CORECLR_NEWRELIC_HOME=/app/newrelic \
  -e CORECLR_PROFILER_PATH=/app/newrelic/libNewRelicProfiler.so \
  helloworld-nr-alpine
```

## Test the Application

Once running, test the endpoints:

```bash
# Hello World endpoint
curl http://localhost:8080/

# Health check endpoint
curl http://localhost:8080/health
```

## New Relic Environment Variables

### Required Variables (set these at runtime):

- `NEW_RELIC_LICENSE_KEY` - Your New Relic license key
- `NEW_RELIC_APP_NAME` - Application name in New Relic dashboard

### .NET Profiler Variables (already configured):

- `CORECLR_ENABLE_PROFILING=1` - Enables profiling
- `CORECLR_PROFILER={36032161-FFC0-4B61-B559-F6C5D41BAE5A}` - New Relic profiler GUID
- `CORECLR_NEWRELIC_HOME=/app/newrelic` - New Relic home directory
- `CORECLR_PROFILER_PATH=/app/newrelic/libNewRelicProfiler.so` - Path to profiler library

### Optional Variables:

- `NEW_RELIC_LOG_LEVEL` - Set to `debug`, `info`, `warn`, or `error` (default: info)
- `NEW_RELIC_DISTRIBUTED_TRACING_ENABLED` - Enable distributed tracing (default: true)

## Verify New Relic Integration

1. Start the application
2. Generate some traffic by hitting the endpoints
3. Check your New Relic dashboard (it may take 1-2 minutes for data to appear)
4. Look for your app under APM & Services

## Troubleshooting

If New Relic data isn't appearing:

1. Check logs: `docker-compose logs -f`
2. Verify your license key is correct
3. Set `NEW_RELIC_LOG_LEVEL=debug` for verbose logging
4. Ensure the profiler path is correct for Alpine: `libNewRelicProfiler.so`

## Notes

- This uses the Alpine-based .NET 8.0 runtime for smaller image size
- The New Relic agent (v10.29.0) is compatible with Alpine Linux
- The profiler library path is specific to Alpine (`.so` file)
EOF

echo "✅ All files created successfully!"
echo ""
echo "Next steps:"
echo "1. Copy .env.example to .env:  cp .env.example .env"
echo "2. Edit .env and add your New Relic license key"
echo "3. Run: docker-compose up --build"
echo ""
echo "Happy coding! 🚀"
