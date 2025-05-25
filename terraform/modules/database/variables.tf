variable "app_name" {
  description = "Application name"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "uppdragsradarn"
}

variable "db_user" {
  description = "Database user"
  type        = string
  default     = "uppdragsradarn"
}

variable "db_password" {
  description = "Database password"
  type        = string
  sensitive   = true
}

variable "db_storage_size" {
  description = "Database storage size"
  type        = string
  default     = "10Gi"
}