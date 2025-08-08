#!/bin/bash
# start-with-listener.sh - Startup script that launches both the main application and the listener agent

set -e

# Configuration
LISTENER_JAR="/app/listener-agent.jar"
MAIN_APP_JAR="/app/main-application.jar"
LISTENER_LOG="/var/log/listener-agent.log"

# Validate environment variables
if [ -z "$TEAM_NAME" ]; then
    echo "ERROR: TEAM_NAME environment variable is required"
    exit 1
fi

if [ -z "$APP_NAME" ]; then
    echo "ERROR: APP_NAME environment variable is required"
    exit 1
fi

if [ -z "$CENTRAL_API_URL" ]; then
    echo "ERROR: CENTRAL_API_URL environment variable is required"
    exit 1
fi

# Set default values if not provided
export POD_NAME=${POD_NAME:-$(hostname)}
export CONTAINER_NAME=${CONTAINER_NAME:-"main"}
export DUMP_DIRECTORY=${DUMP_DIRECTORY:-"/tmp/dumps"}

# Create dump directory
mkdir -p "$DUMP_DIRECTORY"

echo "Starting Container Monitoring System..."
echo "Team: $TEAM_NAME"
echo "App: $APP_NAME"
echo "Pod: $POD_NAME"
echo "Container: $CONTAINER_NAME"
echo "Central API: $CENTRAL_API_URL"

# Function to handle graceful shutdown
cleanup() {
    echo "Received shutdown signal, stopping services..."
    
    # Kill listener agent if running
    if [ ! -z "$LISTENER_PID" ]; then
        echo "Stopping listener agent (PID: $LISTENER_PID)"
        kill -TERM "$LISTENER_PID" 2>/dev/null || true
        wait "$LISTENER_PID" 2>/dev/null || true
    fi
    
    # Kill main application if running
    if [ ! -z "$MAIN_APP_PID" ]; then
        echo "Stopping main application (PID: $MAIN_APP_PID)"
        kill -TERM "$MAIN_APP_PID" 2>/dev/null || true
        wait "$MAIN_APP_PID" 2>/dev/null || true
    fi
    
    echo "Cleanup completed"
    exit 0
}

# Set up signal handlers
trap cleanup SIGTERM SIGINT SIGQUIT

# Start listener agent in background
echo "Starting listener agent..."
java -Xms64m -Xmx128m \
     -Dlogging.file.name="$LISTENER_LOG" \
     -jar "$LISTENER_JAR" &
LISTENER_PID=$!

# Wait a bit for listener to initialize
sleep 5

# Check if listener is still running
if ! kill -0 "$LISTENER_PID" 2>/dev/null; then
    echo "ERROR: Listener agent failed to start"
    cat "$LISTENER_LOG" || true
    exit 1
fi

echo "Listener agent started successfully (PID: $LISTENER_PID)"

# Start main application
echo "Starting main application..."
if [ -f "$MAIN_APP_JAR" ]; then
    # If we have a separate main application jar
    java -jar "$MAIN_APP_JAR" &
    MAIN_APP_PID=$!
else
    # If main application is started via CMD/ENTRYPOINT args
    exec "$@" &
    MAIN_APP_PID=$!
fi

echo "Main application started (PID: $MAIN_APP_PID)"

# Function to check if processes are still running
check_processes() {
    local listener_running=true
    local app_running=true
    
    # Check listener
    if ! kill -0 "$LISTENER_PID" 2>/dev/null; then
        listener_running=false
        echo "WARNING: Listener agent has stopped"
    fi
    
    # Check main app
    if ! kill -0 "$MAIN_APP_PID" 2>/dev/null; then
        app_running=false
        echo "WARNING: Main application has stopped"
    fi
    
    # If main app dies, we should exit (this is the primary process)
    if [ "$app_running" = false ]; then
        echo "Main application has died, shutting down container"
        cleanup
        exit 1
    fi
    
    # If listener dies, try to restart it
    if [ "$listener_running" = false ]; then
        echo "Attempting to restart listener agent..."
        java -Xms64m -Xmx128m \
             -Dlogging.file.name="$LISTENER_LOG" \
             -jar "$LISTENER_JAR" &
        LISTENER_PID=$!
        sleep 2
        
        if kill -0 "$LISTENER_PID" 2>/dev/null; then
            echo "Listener agent restarted successfully (PID: $LISTENER_PID)"
        else
            echo "Failed to restart listener agent"
        fi
    fi
}

# Monitor processes
echo "Monitoring processes..."
while true; do
    check_processes
    sleep 30
done