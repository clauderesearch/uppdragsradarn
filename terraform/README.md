# Uppdragsradarn Terraform Infrastructure

This directory contains the Terraform configuration for deploying Uppdragsradarn to DigitalOcean Kubernetes.

## Architecture

The infrastructure consists of:
- **PostgreSQL Database**: Persistent storage for assignments and user data
- **Redis**: Session management and caching
- **Backend API**: Spring Boot application
- **Frontend**: Nuxt.js public-facing application
- **Admin Panel**: Nuxt.js admin interface
- **Nginx Ingress**: Routing and SSL termination

## Prerequisites

1. Terraform >= 1.5.0
2. DigitalOcean account with:
   - Kubernetes cluster
   - Container Registry
   - Spaces bucket for Terraform state
3. AWS Cognito OAuth2 configuration
4. OpenAI API key

## Directory Structure

```
terraform/
├── environments/
│   ├── dev/         # Development environment
│   └── prod/        # Production environment
├── modules/
│   ├── database/    # PostgreSQL deployment
│   ├── redis/       # Redis deployment
│   └── k8s-apps/    # Application deployments
└── README.md
```

## Usage

### Development Deployment

1. Navigate to the dev environment:
   ```bash
   cd environments/dev
   ```

2. Create a `terraform.tfvars` file from the example:
   ```bash
   cp terraform.tfvars.example terraform.tfvars
   ```

3. Edit `terraform.tfvars` with your actual values

4. Initialize Terraform:
   ```bash
   terraform init \
     -backend-config="access_key=YOUR_SPACES_ACCESS_KEY" \
     -backend-config="secret_key=YOUR_SPACES_SECRET_KEY"
   ```

5. Plan the deployment:
   ```bash
   terraform plan
   ```

6. Apply the configuration:
   ```bash
   terraform apply
   ```

### Required Variables

- `do_token`: DigitalOcean API token
- `aws_cognito_domain`: Cognito domain for OAuth2
- `aws_cognito_client_id`: OAuth2 client ID
- `aws_cognito_client_secret`: OAuth2 client secret
- `openai_api_key`: OpenAI API key for LLM extraction
- `db_password`: PostgreSQL password
- `backend_image`: Backend Docker image URL
- `frontend_image`: Frontend Docker image URL
- `admin_image`: Admin Docker image URL

### State Management

Terraform state is stored in DigitalOcean Spaces. Ensure you have:
1. Created a Spaces bucket named `uppdragsradarn-terraform`
2. Generated access keys for the bucket
3. Configured backend access in terraform init

## CI/CD Integration

The GitHub Actions workflow automatically:
1. Runs tests for all components
2. Builds Docker images
3. Pushes to DigitalOcean Container Registry
4. Deploys to Kubernetes using Terraform

Required GitHub Secrets:
- `DIGITALOCEAN_ACCESS_TOKEN`
- `K8S_CLUSTER_ID`
- `SPACES_ACCESS_KEY`
- `SPACES_SECRET_KEY`
- `AWS_COGNITO_DOMAIN`
- `AWS_COGNITO_CLIENT_ID`
- `AWS_COGNITO_CLIENT_SECRET`
- `OPENAI_API_KEY`
- `DB_PASSWORD`

## Monitoring

After deployment, you can monitor the applications:

```bash
# Check pod status
kubectl get pods -n uppdragsradarn-dev

# View logs
kubectl logs -n uppdragsradarn-dev deployment/uppdragsradarn-backend

# Check ingress
kubectl get ingress -n uppdragsradarn-dev
```

## Troubleshooting

1. **Database Connection Issues**: Check that the database pod is running and the password is correct
2. **Ingress Not Working**: Ensure cert-manager is installed and the domain DNS points to the cluster
3. **Image Pull Errors**: Verify the container registry credentials are configured correctly