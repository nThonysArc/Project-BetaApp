# ============================================
# Etapa 1: build con Maven + JDK 21
# ============================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cachear dependencias en una capa separada del codigo fuente
COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================
# Etapa 2: imagen de ejecucion, liviana
# ============================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

COPY --from=build /app/target/avance-produccion.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
