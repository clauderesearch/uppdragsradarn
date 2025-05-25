variable "do_token" {
  description = "DigitalOcean API Token"
  type        = string
  sensitive   = true
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "app_name" {
  description = "Application name"
  type        = string
  default     = "uppdragsradarn"
}

variable "domain" {
  description = "Application domain"
  type        = string
}

variable "aws_cognito_domain" {
  description = "AWS Cognito domain"
  type        = string
}

variable "aws_cognito_client_id" {
  description = "AWS Cognito client ID"
  type        = string
  sensitive   = true
}

variable "aws_cognito_client_secret" {
  description = "AWS Cognito client secret"
  type        = string
  sensitive   = true
}

variable "openai_api_key" {
  description = "OpenAI API key"
  type        = string
  sensitive   = true
}

variable "db_password" {
  description = "Database password"
  type        = string
  sensitive   = true
}

variable "backend_image" {
  description = "Backend Docker image"
  type        = string
}

variable "frontend_image" {
  description = "Frontend Docker image"
  type        = string
}

variable "admin_image" {
  description = "Admin Docker image"
  type        = string
}