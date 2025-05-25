# UppdragsRadarn

UppdragsRadarn is a platform that helps consultants find and manage assignment opportunities in one place.

## Architecture

- **Backend**: Spring Boot 3 REST API
- **Frontend**: Nuxt3/Vue.js with Tailwind CSS
- **Authentication**: AWS Cognito OAuth2
- **Database**: PostgreSQL
- **Search Engine**: Elasticsearch
- **Infrastructure**: Digital Ocean Kubernetes + Terraform
- **CI/CD**: GitHub Actions

## Project Structure

- `/` - Root directory containing the project configuration
- `/backend` - Spring Boot backend API
- `/frontend` - Nuxt.js frontend application

## Backend Development

### Prerequisites

- Java 21+
- Maven
- Docker and Docker Compose

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/Bytelope/uppdragsradarn.git
   cd uppdragsradar
   ```

2. Configure Environment Variables:
   ```bash
   # Copy the example environment file
   cp .env.local.example .env.local
   
   # Edit the file with your configuration
   nano .env.local
   ```

   Required environment variables:
   - `SPRING_DATASOURCE_URL`: PostgreSQL connection URL (default: jdbc:postgresql://localhost:5432/uppdragsradarn)
   - `SPRING_DATASOURCE_USERNAME`: Database username (default: postgres)
   - `SPRING_DATASOURCE_PASSWORD`: Database password (default: postgres)
   - `COGNITO_CLIENT_ID`: Your AWS Cognito client ID
   - `COGNITO_CLIENT_SECRET`: Your AWS Cognito client secret
   - `COGNITO_REGION`: AWS region where your Cognito user pool is located (e.g., eu-north-1)
   - `COGNITO_USER_POOL_ID`: Your AWS Cognito user pool ID

3. Run the application:
   ```bash
   source ./load-env.sh && cd backend && ./mvnw spring-boot:run
   ```

   The application will automatically start required Docker containers (PostgreSQL and Elasticsearch) using Spring Boot's Docker Compose support.

### API Documentation

API documentation is automatically generated using SpringDoc OpenAPI:
- OpenAPI JSON: http://localhost/api/v3/api-docs
- Swagger UI: http://localhost/api/swagger-ui.html

## Frontend Development

### Prerequisites

- Node.js 18+
- npm or yarn

### Setup

1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   # or
   yarn install
   ```

3. Configure environment variables:
   ```bash
   # Create .env file
   cp .env.example .env
   
   # Edit with your values
   nano .env
   ```

   Required environment variables:
   - `NUXT_PUBLIC_API_BASE`: URL of the backend API (default: http://localhost/api)
   - `NUXT_PUBLIC_COGNITO_REGION`: AWS Cognito region
   - `NUXT_PUBLIC_COGNITO_USER_POOL_ID`: AWS Cognito User Pool ID
   - `NUXT_PUBLIC_COGNITO_CLIENT_ID`: AWS Cognito Client ID

4. Start the development server:
   ```bash
   npm run dev
   # or
   yarn dev
   ```

   The frontend will be available at: http://localhost:3000

## Docker Compose Integration

The application uses Spring Boot's Docker Compose support to automatically manage required infrastructure:

- **PostgreSQL**: Database for storing application data
- **Elasticsearch**: Search engine for efficient assignment searching

When you start the application with `./mvnw spring-boot:run`, these services will be automatically started.

To manually control the Docker containers:

```bash
# Start containers
docker-compose -f compose.yaml up -d

# Stop containers
docker-compose -f compose.yaml down

# View container logs
docker-compose -f compose.yaml logs -f
```

## Local Development Setup

For local development, HTTPS is required for AWS Cognito OAuth2 authentication to work properly.

### Initial Setup (Run once)

1. Run the setup script to configure hosts and generate SSL certificates:
   ```bash
   sudo ./setup-dev-hosts.sh
   ```

   This script will:
   - Add local development domains to `/etc/hosts`
   - Install mkcert for local SSL certificate generation
   - Generate trusted SSL certificates for `*.dev.uppdragsradarn.se`
   - Create certificates in `backend/certs/`

## Running the Complete Application

To run the complete application (backend and frontend):

1. Start the backend:
   ```bash
   cd backend && ./start.sh
   ```

   This will:
   - Check that SSL certificates exist (prompts to run setup if missing)
   - Start PostgreSQL and Nginx with HTTPS enabled
   - Launch the Spring Boot application

2. In separate terminals, start the frontend applications:
   ```bash
   cd frontend && npm run dev  # Main frontend
   cd admin && npm run dev     # Admin panel (in another terminal)
   ```

3. Access the applications via HTTPS:
   - Frontend: https://dev.uppdragsradarn.se
   - Admin: https://admin.dev.uppdragsradarn.se
   - API: https://api.dev.uppdragsradarn.se

Note: HTTP requests will be automatically redirected to HTTPS.

## Deployment

### Kubernetes

The application is designed to be deployed on Kubernetes. Kubernetes manifests are available in the `k8s` directory.

### Infrastructure as Code

Terraform is used for infrastructure provisioning. Terraform configuration is available in the `terraform` directory.

## License

This project is licensed under the MIT License - see the LICENSE file for details.# Trigger deployment
