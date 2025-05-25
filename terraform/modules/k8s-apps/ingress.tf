resource "kubernetes_ingress_v1" "app" {
  metadata {
    name      = "${var.app_name}-ingress"
    namespace = kubernetes_namespace.app.metadata[0].name
    annotations = {
      "cert-manager.io/cluster-issuer" = "letsencrypt-prod"
      "nginx.ingress.kubernetes.io/proxy-body-size" = "50m"
      "nginx.ingress.kubernetes.io/proxy-read-timeout" = "600"
      "nginx.ingress.kubernetes.io/proxy-send-timeout" = "600"
      "nginx.ingress.kubernetes.io/whitelist-source-range" = "37.46.168.212/32"
      "nginx.ingress.kubernetes.io/ssl-redirect" = "true"
    }
  }

  spec {
    ingress_class_name = "nginx"

    tls {
      hosts = [var.domain]
      secret_name = "${var.app_name}-tls"
    }

    rule {
      host = var.domain
      
      http {
        path {
          path = "/api"
          path_type = "Prefix"
          
          backend {
            service {
              name = kubernetes_service.backend.metadata[0].name
              port {
                number = 8080
              }
            }
          }
        }

        path {
          path = "/admin"
          path_type = "Prefix"
          
          backend {
            service {
              name = kubernetes_service.frontend_proxy.metadata[0].name
              port {
                number = 80
              }
            }
          }
        }

        path {
          path = "/"
          path_type = "Prefix"
          
          backend {
            service {
              name = kubernetes_service.frontend_proxy.metadata[0].name
              port {
                number = 80
              }
            }
          }
        }
      }
    }
  }
}