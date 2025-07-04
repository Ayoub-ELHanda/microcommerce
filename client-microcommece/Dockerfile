FROM openjdk:17-jdk-slim

LABEL maintainer="Client Microservice"
LABEL description="E-commerce Client Service with RabbitMQ integration"

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application
RUN ./mvnw clean package -DskipTests

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8082/api/clients || exit 1

# Run the application
CMD ["java", "-jar", "target/client-microcommerce-0.0.1-SNAPSHOT.jar"] 