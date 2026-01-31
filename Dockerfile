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

#  런타임 이미지
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
