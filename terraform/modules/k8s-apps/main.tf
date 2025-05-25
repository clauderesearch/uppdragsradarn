resource "kubernetes_namespace" "app" {
  metadata {
    name = "${var.app_name}-${var.environment}"
  }
}

resource "kubernetes_config_map" "app_config" {
  metadata {
    name      = "${var.app_name}-config"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  data = {
    SPRING_PROFILES_ACTIVE = var.environment
    SPRING_DATASOURCE_URL  = "jdbc:postgresql://${var.db_host}:${var.db_port}/${var.db_name}"
    SPRING_DATA_REDIS_HOST = var.redis_host
    SPRING_DATA_REDIS_PORT = tostring(var.redis_port)
    OAUTH2_BASE_URI        = "https://${var.domain}"
    AWS_COGNITO_DOMAIN     = var.aws_cognito_domain
  }
}

resource "kubernetes_secret" "app_secrets" {
  metadata {
    name      = "${var.app_name}-secrets"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  data = {
    SPRING_DATASOURCE_USERNAME     = var.db_user
    SPRING_DATASOURCE_PASSWORD     = var.db_password
    AWS_COGNITO_CLIENT_ID         = var.aws_cognito_client_id
    AWS_COGNITO_CLIENT_SECRET     = var.aws_cognito_client_secret
    OPENAI_API_KEY               = var.openai_api_key
  }
}

resource "kubernetes_deployment" "backend" {
  metadata {
    name      = "${var.app_name}-backend"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  spec {
    replicas = var.backend_replicas

    selector {
      match_labels = {
        app = "${var.app_name}-backend"
      }
    }

    template {
      metadata {
        labels = {
          app = "${var.app_name}-backend"
        }
      }

      spec {
        container {
          image = var.backend_image
          name  = "backend"

          port {
            container_port = 8080
          }

          env_from {
            config_map_ref {
              name = kubernetes_config_map.app_config.metadata[0].name
            }
          }

          env_from {
            secret_ref {
              name = kubernetes_secret.app_secrets.metadata[0].name
            }
          }

          liveness_probe {
            http_get {
              path = "/actuator/health"
              port = 8080
            }
            initial_delay_seconds = 120
            period_seconds        = 10
          }

          readiness_probe {
            http_get {
              path = "/actuator/health"
              port = 8080
            }
            initial_delay_seconds = 30
            period_seconds        = 5
          }

          resources {
            requests = {
              cpu    = "500m"
              memory = "1Gi"
            }
            limits = {
              cpu    = "1000m"
              memory = "2Gi"
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "backend" {
  metadata {
    name      = "${var.app_name}-backend"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  spec {
    selector = {
      app = "${var.app_name}-backend"
    }

    port {
      port        = 8080
      target_port = 8080
    }

    type = "ClusterIP"
  }
}

resource "kubernetes_deployment" "frontend" {
  metadata {
    name      = "${var.app_name}-frontend"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  spec {
    replicas = var.frontend_replicas

    selector {
      match_labels = {
        app = "${var.app_name}-frontend"
      }
    }

    template {
      metadata {
        labels = {
          app = "${var.app_name}-frontend"
        }
      }

      spec {
        container {
          image = var.frontend_image
          name  = "frontend"

          port {
            container_port = 3000
          }

          env {
            name  = "NUXT_PUBLIC_API_BASE_URL"
            value = "https://${var.domain}/api"
          }

          env {
            name  = "NUXT_PUBLIC_AUTH_BASE_URL"
            value = "https://${var.domain}"
          }

          liveness_probe {
            http_get {
              path = "/"
              port = 3000
            }
            initial_delay_seconds = 30
            period_seconds        = 10
          }

          resources {
            requests = {
              cpu    = "100m"
              memory = "256Mi"
            }
            limits = {
              cpu    = "500m"
              memory = "512Mi"
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "frontend" {
  metadata {
    name      = "${var.app_name}-frontend"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  spec {
    selector = {
      app = "${var.app_name}-frontend"
    }

    port {
      port        = 3000
      target_port = 3000
    }

    type = "ClusterIP"
  }
}

resource "kubernetes_deployment" "admin" {
  metadata {
    name      = "${var.app_name}-admin"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  spec {
    replicas = var.admin_replicas

    selector {
      match_labels = {
        app = "${var.app_name}-admin"
      }
    }

    template {
      metadata {
        labels = {
          app = "${var.app_name}-admin"
        }
      }

      spec {
        container {
          image = var.admin_image
          name  = "admin"

          port {
            container_port = 3000
          }

          env {
            name  = "NUXT_PUBLIC_API_BASE_URL"
            value = "https://${var.domain}/api"
          }

          env {
            name  = "NUXT_PUBLIC_AUTH_BASE_URL"
            value = "https://${var.domain}"
          }

          liveness_probe {
            http_get {
              path = "/"
              port = 3000
            }
            initial_delay_seconds = 30
            period_seconds        = 10
          }

          resources {
            requests = {
              cpu    = "100m"
              memory = "256Mi"
            }
            limits = {
              cpu    = "500m"
              memory = "512Mi"
            }
          }
        }
      }
    }
  }
}

resource "kubernetes_service" "admin" {
  metadata {
    name      = "${var.app_name}-admin"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  spec {
    selector = {
      app = "${var.app_name}-admin"
    }

    port {
      port        = 3000
      target_port = 3000
    }

    type = "ClusterIP"
  }
}