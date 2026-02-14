# ============================================================
# Stage 1: Build
# Eclipse Temurin JDK 17 on Alpine (discarded after build)
# ============================================================
FROM eclipse-temurin:17-jdk-alpine AS build

RUN apk add --no-cache bash gcompat

WORKDIR /app

# -- Layer cache: Maven wrapper --
COPY .mvn/ .mvn/
COPY mvnw ./
RUN chmod +x mvnw

# -- Layer cache: Maven dependencies --
COPY pom.xml ./
RUN ./mvnw dependency:resolve dependency:resolve-plugins -B -q

# -- Copy source and build --
COPY src/ src/
RUN ./mvnw package -DskipTests -B -q

# -- Extract layered JAR --
RUN java -Djarmode=layertools -jar target/wego-1.0.0-SNAPSHOT.jar extract \
    --destination /app/extracted

# ============================================================
# Stage 2: Runtime
# Eclipse Temurin JRE 17 on Alpine (~130MB base)
# ============================================================
FROM eclipse-temurin:17-jre-alpine AS runtime

RUN addgroup -S wego && adduser -S wego -G wego

WORKDIR /app

# -- Copy layered JAR (least → most frequently changed) --
COPY --from=build /app/extracted/dependencies/ ./
COPY --from=build /app/extracted/spring-boot-loader/ ./
COPY --from=build /app/extracted/snapshot-dependencies/ ./
COPY --from=build /app/extracted/application/ ./

RUN chown -R wego:wego /app
USER wego

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom -Dspring.profiles.active=prod"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
