#!/bin/bash

# Script to regenerate OpenAPI spec and update all API clients
# This will:
# 1. Generate the OpenAPI spec from the Spring Boot app
# 2. Generate API clients for frontend and admin apps

echo "===== Regenerating all API clients ====="

BASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.."
API_SPEC_DIR="$BASE_DIR/api-spec"
FRONTEND_DIR="$BASE_DIR/frontend"
ADMIN_DIR="$BASE_DIR/admin"

# Generate OpenAPI spec
echo "Step 1: Generating OpenAPI specification..."
"$API_SPEC_DIR/generate-openapi.sh"
if [ $? -ne 0 ]; then
    echo "Error generating OpenAPI specification. Exiting."
    exit 1
fi

# Generate frontend API client
echo "Step 2: Generating frontend API client..."
cd "$FRONTEND_DIR" && npx @hey-api/openapi-ts -c @hey-api/client-fetch -i ../api-spec/openapi.json -o ./api-client
if [ $? -ne 0 ]; then
    echo "Error generating frontend API client. Exiting."
    exit 1
fi

# Generate admin API client
echo "Step 3: Generating admin API client..."
cd "$ADMIN_DIR" && npx @hey-api/openapi-ts -c @hey-api/client-fetch -i ../api-spec/openapi.json -o ./api-client
if [ $? -ne 0 ]; then
    echo "Error generating admin API client. Exiting."
    exit 1
fi

echo "===== API Client Regeneration Complete ====="
echo "All API clients have been successfully regenerated."
echo ""
echo "You can now restart your dev servers if they are running."

exit 0