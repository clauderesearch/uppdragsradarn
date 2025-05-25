terraform {
  backend "s3" {
    endpoints = {
      s3 = "https://ams3.digitaloceanspaces.com"
    }
    key                        = "terraform/prod/terraform.tfstate"
    bucket                     = "uppdragsradarn-terraform"
    region                     = "us-east-1"
    skip_credentials_validation = true
    skip_metadata_api_check    = true
    skip_requesting_account_id = true
    skip_s3_checksum           = true
  }
}

module "database" {
  source = "../../modules/database"
  
  app_name        = var.app_name
  environment     = var.environment
  db_password     = var.db_password
  db_storage_size = "50Gi"
}

module "redis" {
  source = "../../modules/redis"
  
  app_name    = var.app_name
  environment = var.environment
}

module "spaces" {
  source = "../../modules/spaces"
  
  frontend_bucket_name = "${var.app_name}-frontend-${var.environment}"
  admin_bucket_name    = "${var.app_name}-admin-${var.environment}"
  region              = "ams3"
}

module "apps" {
  source = "../../modules/k8s-apps"
  
  app_name                  = var.app_name
  environment               = var.environment
  domain                    = var.domain
  db_host                   = module.database.postgres_host
  db_port                   = module.database.postgres_port
  db_name                   = "uppdragsradarn"
  db_user                   = "uppdragsradarn"
  db_password               = var.db_password
  redis_host                = module.redis.redis_host
  redis_port                = module.redis.redis_port
  aws_cognito_domain        = var.aws_cognito_domain
  aws_cognito_client_id     = var.aws_cognito_client_id
  aws_cognito_client_secret = var.aws_cognito_client_secret
  openai_api_key           = var.openai_api_key
  backend_image            = var.backend_image
  backend_replicas         = 2
  frontend_bucket_domain   = module.spaces.frontend_bucket_domain
  admin_bucket_domain      = module.spaces.admin_bucket_domain
}