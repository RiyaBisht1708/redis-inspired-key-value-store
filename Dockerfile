
FROM eclipse-temurin:24-jdk-alpine AS build

WORKDIR /app

RUN apk add --no-cache maven

COPY pom.xml .
COPY src src

RUN mvn -B package -DskipTests

FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

COPY --from=build /app/target/codecrafters-redis.jar ./codecrafters-redis.jar

ENTRYPOINT ["java", "-jar", "codecrafters-redis.jar"]