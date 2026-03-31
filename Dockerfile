# Bước 1: Build dự án bằng Maven
FROM maven:3.8.4-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Bước 2: Chạy file JAR (Dùng ảnh của Amazon Corretto cực kỳ ổn định)
FROM amazoncorretto:17-alpine
COPY --from=build /target/distributedsystem-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]