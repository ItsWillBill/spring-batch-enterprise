# STAGE 1: Build

FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY mvnw .
COPY .mvn/ .mvn/
RUN chmod +x mvnw

COPY pom.xml .

RUN ./mvnw dependency:go-offline -q

COPY src ./src

RUN ./mvnw package -DskipTests --no-transfer-progress
# Extract layers for efficient Docker caching (Spring Boot layared JARs)
RUN java -Djarmode=tools -jar target/reconciliation-engine-*.jar extract --layers --launcher --destination extracted

# STAGE 2: Runtime

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /build/extracted/dependencies/ ./
COPY --from=builder /build/extracted/spring-boot-loader/ ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/ ./

RUN mkdir -p /app/data/input /data/archive && chown -R appuser:appgroup /app /data

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q0- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", "org.springframework.boot.loader.launch.JarLauncher"]