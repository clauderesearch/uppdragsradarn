output "postgres_host" {
  value = "${kubernetes_service.postgres.metadata[0].name}.${kubernetes_namespace.database.metadata[0].name}.svc.cluster.local"
}

output "postgres_port" {
  value = 5432
}

output "namespace" {
  value = kubernetes_namespace.database.metadata[0].name
}