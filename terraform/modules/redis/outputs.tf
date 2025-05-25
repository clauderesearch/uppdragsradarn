output "redis_host" {
  value = "${kubernetes_service.redis.metadata[0].name}.${kubernetes_namespace.redis.metadata[0].name}.svc.cluster.local"
}

output "redis_port" {
  value = 6379
}

output "namespace" {
  value = kubernetes_namespace.redis.metadata[0].name
}