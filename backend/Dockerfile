FROM eclipse-temurin:17-jre-alpine

# Add a spring user to run our application
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Set spring as the owner of the jar file
COPY --chown=spring:spring target/*.jar app.jar

# Configure a healthcheck
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -q --spider http://localhost:8081/actuator/health/ || exit 1

# Use spring as the user to run the application
USER spring:spring

# Run the application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]