# -------- Build stage --------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY . .
RUN mvn -q -DskipTests package

# -------- Run stage --------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Limita memoria para instancias pequeñas de Render
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# Render usa variable de entorno $PORT
CMD ["sh", "-c", "java -Dserver.port=$PORT -jar app.jar"]
