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
    SPRING_PROFILES_ACTIVE                 = var.environment
    SPRING_DATASOURCE_URL                  = "jdbc:postgresql://${var.db_host}:${var.db_port}/${var.db_name}"
    SPRING_DATASOURCE_USERNAME             = var.db_user
    SPRING_DATA_REDIS_HOST                 = var.redis_host
    SPRING_DATA_REDIS_PORT                 = tostring(var.redis_port)
    SERVER_FORWARD_HEADERS_STRATEGY        = "FRAMEWORK"
    SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_COGNITO_REDIRECT_URI = "https://${var.domain}/auth/callback"
    AWS_COGNITO_DOMAIN                     = var.aws_cognito_domain
    COGNITO_REGION                         = "eu-north-1"
    COGNITO_USER_POOL_ID                   = "eu-north-1_34ZSxWwnh"
    COGNITO_ISSUER_URI                     = "https://cognito-idp.eu-north-1.amazonaws.com/eu-north-1_34ZSxWwnh"
  }
}

resource "kubernetes_secret" "app_secrets" {
  metadata {
    name      = "${var.app_name}-secrets"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  data = {
    SPRING_DATASOURCE_PASSWORD = var.db_password
    COGNITO_CLIENT_ID          = var.aws_cognito_client_id
    COGNITO_CLIENT_SECRET      = var.aws_cognito_client_secret
    OPENAI_API_KEY            = var.openai_api_key
  }
}

resource "kubernetes_config_map" "frontend_urls" {
  metadata {
    name      = "frontend-urls"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  data = {
    FRONTEND_URL = "https://${var.frontend_bucket_domain}"
    ADMIN_URL    = "https://${var.admin_bucket_domain}"
  }
}

resource "kubernetes_deployment" "backend" {
  metadata {
    name      = "${var.app_name}-backend"
    namespace = kubernetes_namespace.app.metadata[0].name
    labels = {
      app = "${var.app_name}-backend"
    }
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
        init_container {
          name  = "wait-for-database"
          image = "busybox:1.35"
          command = [
            "sh", "-c",
            "until nc -z ${var.db_host} ${var.db_port}; do echo waiting for database; sleep 2; done"
          ]
        }

        container {
          name  = "backend"
          image = var.backend_image

          port {
            container_port = 8080
            name          = "http"
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
            initial_delay_seconds = 60
            period_seconds       = 10
            timeout_seconds      = 5
            failure_threshold    = 3
          }

          readiness_probe {
            http_get {
              path = "/actuator/health"
              port = 8080
            }
            initial_delay_seconds = 30
            period_seconds       = 5
            timeout_seconds      = 3
            failure_threshold    = 3
          }

          resources {
            requests = {
              memory = "512Mi"
              cpu    = "250m"
            }
            limits = {
              memory = "1Gi"
              cpu    = "500m"
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
      app = kubernetes_deployment.backend.metadata[0].labels.app
    }

    port {
      port        = 8080
      target_port = 8080
      protocol    = "TCP"
    }

    type = "ClusterIP"
  }
}

# Create a simple nginx deployment to handle redirects to Spaces
resource "kubernetes_deployment" "frontend_proxy" {
  metadata {
    name      = "${var.app_name}-frontend-proxy"
    namespace = kubernetes_namespace.app.metadata[0].name
    labels = {
      app = "${var.app_name}-frontend-proxy"
    }
  }

  spec {
    replicas = 1

    selector {
      match_labels = {
        app = "${var.app_name}-frontend-proxy"
      }
    }

    template {
      metadata {
        labels = {
          app = "${var.app_name}-frontend-proxy"
        }
      }

      spec {
        container {
          name  = "nginx"
          image = "nginx:alpine"

          port {
            container_port = 80
          }

          volume_mount {
            name       = "nginx-config"
            mount_path = "/etc/nginx/nginx.conf"
            sub_path   = "nginx.conf"
          }
        }

        volume {
          name = "nginx-config"
          config_map {
            name = kubernetes_config_map.nginx_config.metadata[0].name
          }
        }
      }
    }
  }
}

resource "kubernetes_config_map" "nginx_config" {
  metadata {
    name      = "nginx-config"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  data = {
    "nginx.conf" = <<-EOF
      events {
        worker_connections 1024;
      }
      
      http {
        server {
          listen 80;
          server_name _;
          
          # Redirect frontend requests to Spaces
          location / {
            return 301 https://${var.frontend_bucket_domain}$request_uri;
          }
          
          # Redirect admin requests to Spaces
          location /admin {
            return 301 https://${var.admin_bucket_domain}$request_uri;
          }
          
          # Health check
          location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
          }
        }
      }
    EOF
  }
}

resource "kubernetes_service" "frontend_proxy" {
  metadata {
    name      = "${var.app_name}-frontend-proxy"
    namespace = kubernetes_namespace.app.metadata[0].name
  }

  spec {
    selector = {
      app = kubernetes_deployment.frontend_proxy.metadata[0].labels.app
    }

    port {
      port        = 80
      target_port = 80
      protocol    = "TCP"
    }

    type = "ClusterIP"
  }
}