terraform {
  backend "s3" {
    endpoint                    = "ams3.digitaloceanspaces.com"
    key                        = "terraform/dev/terraform.tfstate"
    bucket                     = "uppdragsradarn-terraform"
    region                     = "us-east-1"
    skip_credentials_validation = true
    skip_metadata_api_check    = true
  }
}

module "database" {
  source = "../../modules/database"
  
  app_name    = var.app_name
  environment = var.environment
  db_password = var.db_password
}

module "redis" {
  source = "../../modules/redis"
  
  app_name    = var.app_name
  environment = var.environment
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
  frontend_image           = var.frontend_image
  admin_image              = var.admin_image
  backend_replicas         = 1
  frontend_replicas        = 1
  admin_replicas           = 1
}