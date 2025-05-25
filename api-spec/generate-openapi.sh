#!/bin/bash

# Script to generate OpenAPI spec from the Spring Boot application
# This will start the Spring Boot app, wait for it to initialize,
# download the OpenAPI spec, and then shut down the app

# Set variables
BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.."
BACKEND_DIR="$BASE_DIR/backend"
API_SPEC_DIR="$BASE_DIR/api-spec"
OPENAPI_URL="http://localhost:8081/v3/api-docs"
OUTPUT_FILE="$API_SPEC_DIR/openapi.json"
LOG_FILE="./spring-boot-openapi-generator.log"
WAIT_TIME=60  # seconds to wait for Spring Boot to start

echo "===== OpenAPI Specification Generator ====="
echo "Starting from directory: $BASE_DIR"

# Create directory if it doesn't exist
mkdir -p "$API_SPEC_DIR"

# Start Spring Boot application in background
echo "Starting Spring Boot application..."
cd "$BACKEND_DIR" && source ./load-env.sh && ./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=dev > "$LOG_FILE" 2>&1 &
SPRING_PID=$!

# Function to clean up and exit
cleanup() {
    echo "Stopping Spring Boot application (PID: $SPRING_PID)..."
    kill -15 $SPRING_PID 2>/dev/null || true
    echo "Cleaning up temporary files..."
    rm -f "$LOG_FILE"
    exit 1
}

# Set up trap to clean up on script termination
trap cleanup INT TERM EXIT

# Wait for application to start
echo "Waiting for application to start (this will take about $WAIT_TIME seconds)..."
sleep $WAIT_TIME

# Check if the application started successfully
if ! ps -p $SPRING_PID > /dev/null; then
    echo "ERROR: Spring Boot application failed to start. Check the logs at $LOG_FILE"
    exit 1
fi

# Download OpenAPI spec
echo "Downloading OpenAPI specification from $OPENAPI_URL..."
if curl -s -o "$OUTPUT_FILE" "$OPENAPI_URL"; then
    echo "OpenAPI specification downloaded successfully to $OUTPUT_FILE"
else
    echo "ERROR: Failed to download OpenAPI specification. Check if the Spring Boot app is running."
    echo "You can view the logs at $LOG_FILE"
    exit 1
fi

# Stop the Spring Boot application
echo "Stopping Spring Boot application..."
kill -15 $SPRING_PID

# Wait for the application to shut down
wait $SPRING_PID 2>/dev/null || true

# Show success message
echo "===== OpenAPI Specification Generated Successfully ====="
echo "You can find the OpenAPI specification at: $OUTPUT_FILE"
echo ""
echo "Next steps:"
echo "1. Run 'npm run generate-api-client' in the frontend and admin directories"
echo "2. Restart the dev servers if they are running"

# Remove the trap since we've successfully finished
trap - INT TERM EXIT

# Clean up logs
rm -f "$LOG_FILE"

exit 0