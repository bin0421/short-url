# Single image: the built frontend is served by the Spring Boot app as static resources.

FROM node:24-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM eclipse-temurin:21-jdk AS backend
WORKDIR /backend
COPY backend/gradlew ./
COPY backend/gradle gradle
COPY backend/build.gradle.kts backend/settings.gradle.kts ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null
COPY backend/src src
COPY --from=frontend /frontend/dist src/main/resources/static
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /backend/build/libs/backend.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
