# ── Stage 1: Backend build ────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS be-build

WORKDIR /app

# Copy pom first so Maven dependency layer is cached independently of source changes
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build (tests run in CI, not here)
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Non-root user for security
RUN addgroup --system reporthole \
 && adduser --system --ingroup reporthole reporthole

# Pre-create upload dir with correct ownership so the volume mount is writable
RUN mkdir -p /app/uploads/incidents \
 && chown -R reporthole:reporthole /app/uploads

USER reporthole

COPY --from=be-build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
