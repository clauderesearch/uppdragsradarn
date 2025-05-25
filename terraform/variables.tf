variable "do_token" {
  description = "DigitalOcean API Token"
  type        = string
  sensitive   = true
}

variable "cluster_id" {
  description = "DigitalOcean Kubernetes Cluster ID"
  type        = string
  default     = "486fe579-8744-4929-9357-ef8bcf9d2d04"
}

variable "environment" {
  description = "Environment name (dev, prod)"
  type        = string
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