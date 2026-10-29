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

# fontconfig + DejaVu: Java2D (generacion del PNG del reporte) las necesita en
# un contenedor minimo, aunque el renderer ya incluya sus propias fuentes.
RUN apt-get update \
 && apt-get install -y --no-install-recommends fontconfig fonts-dejavu-core \
 && rm -rf /var/lib/apt/lists/*

RUN addgroup --system spring && adduser --system --ingroup spring spring
USER spring:spring

# headless: no hay pantalla. MaxRAMPercentage: la JVM respeta el limite del contenedor.
ENV TZ=America/Lima \
    JAVA_TOOL_OPTIONS="-Djava.awt.headless=true -XX:MaxRAMPercentage=75"

# Wildcard en vez de nombre exacto: evita que el build se rompa cada vez
# que cambie la version en pom.xml o el finalName del jar generado.
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
