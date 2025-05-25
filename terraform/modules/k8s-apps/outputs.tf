output "namespace" {
  value = kubernetes_namespace.app.metadata[0].name
}

output "backend_service" {
  value = kubernetes_service.backend.metadata[0].name
}

output "frontend_service" {
  value = kubernetes_service.frontend.metadata[0].name
}

output "admin_service" {
  value = kubernetes_service.admin.metadata[0].name
}