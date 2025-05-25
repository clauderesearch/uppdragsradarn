# API Specification and Client Generation

This directory contains the OpenAPI specification for the UppdragsRadarn API and scripts to automate the generation of API clients.

## Available Scripts

### generate-openapi.sh

Automatically generates the OpenAPI specification from a running Spring Boot application.

```bash
./generate-openapi.sh
```

This script will:
1. Start the Spring Boot application using the dev profile
2. Wait for it to initialize
3. Download the OpenAPI specification from `/v3/api-docs`
4. Save it to `openapi.json` in this directory
5. Shut down the Spring Boot application

### regenerate-all-clients.sh

Regenerates the OpenAPI specification and updates API clients for both frontend and admin applications.

```bash
./regenerate-all-clients.sh
```

This script will:
1. Generate the OpenAPI specification using `generate-openapi.sh`
2. Generate the frontend API client
3. Generate the admin API client

## NPM Scripts

Both the frontend and admin applications have an npm script to generate the OpenAPI specification and update their API clients:

```bash
# In frontend directory
npm run generate-openapi-spec

# In admin directory
npm run generate-openapi-spec
```

## CI/CD Integration

For continuous integration, you can incorporate these scripts into your CI/CD pipeline to ensure API clients are always in sync with the backend API.

## Automatic Generation with Maven

The backend project is configured to automatically generate the OpenAPI specification during the Maven build process using the `springdoc-openapi-maven-plugin`. The generated specification is copied to this directory during the package phase.

To manually trigger this process:

```bash
cd backend
./mvnw package -DskipTests
```

This approach doesn't require starting the application but may not include runtime-generated API endpoints.