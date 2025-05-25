output "frontend_bucket_name" {
  description = "Name of the frontend Spaces bucket"
  value       = digitalocean_spaces_bucket.frontend.name
}

output "admin_bucket_name" {
  description = "Name of the admin Spaces bucket"
  value       = digitalocean_spaces_bucket.admin.name
}

output "frontend_bucket_domain" {
  description = "Domain name of the frontend bucket"
  value       = digitalocean_spaces_bucket.frontend.bucket_domain_name
}

output "admin_bucket_domain" {
  description = "Domain name of the admin bucket"
  value       = digitalocean_spaces_bucket.admin.bucket_domain_name
}

output "frontend_cdn_endpoint" {
  description = "CDN endpoint for frontend"
  value       = digitalocean_cdn.frontend.endpoint
}

output "admin_cdn_endpoint" {
  description = "CDN endpoint for admin"
  value       = digitalocean_cdn.admin.endpoint
}