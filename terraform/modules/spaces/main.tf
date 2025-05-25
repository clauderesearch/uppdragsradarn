terraform {
  required_providers {
    digitalocean = {
      source  = "digitalocean/digitalocean"
      version = "~> 2.0"
    }
  }
}

# Create Spaces bucket for frontend
resource "digitalocean_spaces_bucket" "frontend" {
  name   = var.frontend_bucket_name
  region = var.region

  # Enable static website hosting
  versioning {
    enabled = false
  }

  lifecycle_rule {
    id      = "cleanup-old-versions"
    enabled = true

    expiration {
      days = 90
    }
  }
}

# Create Spaces bucket for admin
resource "digitalocean_spaces_bucket" "admin" {
  name   = var.admin_bucket_name
  region = var.region

  # Enable static website hosting
  versioning {
    enabled = false
  }

  lifecycle_rule {
    id      = "cleanup-old-versions"
    enabled = true

    expiration {
      days = 90
    }
  }
}

# CORS configuration for frontend bucket
resource "digitalocean_spaces_bucket_cors_configuration" "frontend" {
  bucket = digitalocean_spaces_bucket.frontend.name
  region = digitalocean_spaces_bucket.frontend.region

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "HEAD"]
    allowed_origins = ["*"]
    max_age_seconds = 3000
  }
}

# CORS configuration for admin bucket
resource "digitalocean_spaces_bucket_cors_configuration" "admin" {
  bucket = digitalocean_spaces_bucket.admin.name
  region = digitalocean_spaces_bucket.admin.region

  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "HEAD"]
    allowed_origins = ["*"]
    max_age_seconds = 3000
  }
}

# CDN endpoint for frontend
resource "digitalocean_cdn" "frontend" {
  origin           = digitalocean_spaces_bucket.frontend.bucket_domain_name
  custom_domain    = var.frontend_custom_domain
  certificate_name = var.certificate_name
  ttl              = 3600
}

# CDN endpoint for admin
resource "digitalocean_cdn" "admin" {
  origin           = digitalocean_spaces_bucket.admin.bucket_domain_name
  custom_domain    = var.admin_custom_domain
  certificate_name = var.certificate_name
  ttl              = 3600
}