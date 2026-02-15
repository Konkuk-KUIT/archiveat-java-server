FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Gradle wrapper 및 의존성 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

# 소스 코드 복사
COPY src src

# Gradle 빌드 실행
RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon

# 런타임 이미지
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Sentry 환경 변수를 위한 ARG 선언
ARG SENTRY_DSN
ARG SENTRY_ENVIRONMENT=production

# ARG를 ENV로 변환 (런타임에 사용)
ENV SENTRY_DSN=${SENTRY_DSN}
ENV SENTRY_ENVIRONMENT=${SENTRY_ENVIRONMENT}

# curl 설치 (health check용)
RUN apk add --no-cache curl

# 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# Health check 추가
#HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
#    CMD curl -f http://localhost:8080/actuator/health || exit 1
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -fsS http://localhost:8080/ping || exit 1



# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
