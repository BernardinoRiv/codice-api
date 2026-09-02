# Etapa 1: Compilación con Maven
FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Compila el proyecto omitiendo pruebas temporalmente para agilizar el despliegue
RUN mvn clean package -DskipTests

# Etapa 2: Ejecución con JRE ligero
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]