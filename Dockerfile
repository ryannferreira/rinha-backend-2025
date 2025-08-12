FROM eclipse-temurin:21-jdk-jammy as builder

WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./

RUN ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw package -DskipTests


FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

COPY config ./config

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "app.jar"]
