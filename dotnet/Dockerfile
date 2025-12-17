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
