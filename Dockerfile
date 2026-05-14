FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src/ src/
RUN ./mvnw clean package -DskipTests
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "target/leiloai-api-0.0.1-SNAPSHOT.jar"]