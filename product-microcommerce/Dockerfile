FROM openjdk:17-jdk-slim

LABEL maintainer="Product Microservice"
LABEL description="E-commerce Product Service with RabbitMQ integration"

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
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8081/api/products || exit 1

# Run the application
CMD ["java", "-jar", "target/microcommerce-0.0.1-SNAPSHOT.jar"] 