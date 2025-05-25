variable "frontend_bucket_name" {
  description = "Name of the Spaces bucket for frontend"
  type        = string
}

variable "admin_bucket_name" {
  description = "Name of the Spaces bucket for admin"
  type        = string
}

variable "region" {
  description = "DigitalOcean region"
  type        = string
  default     = "ams3"
}

variable "frontend_custom_domain" {
  description = "Custom domain for frontend CDN"
  type        = string
  default     = null
}

variable "admin_custom_domain" {
  description = "Custom domain for admin CDN"
  type        = string
  default     = null
}

variable "certificate_name" {
  description = "Certificate name for CDN"
  type        = string
  default     = null
}