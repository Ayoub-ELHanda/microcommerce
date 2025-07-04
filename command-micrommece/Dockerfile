FROM openjdk:17-jdk-slim

LABEL maintainer="Command Microservice"
LABEL description="E-commerce Command Service with RabbitMQ integration"

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
EXPOSE 8083

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8083/api/commands || exit 1

# Run the application
CMD ["java", "-jar", "target/command-microcommerce-0.0.1-SNAPSHOT.jar"] 