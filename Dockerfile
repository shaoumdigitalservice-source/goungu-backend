# --- Étape 1 : compilation ---
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw package -DskipTests -B

# --- Étape 2 : image d'exécution ---
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Utilisateur non-root pour l'exécution du conteneur
RUN useradd --create-home --shell /bin/false goungue
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R goungue:goungue /app
USER goungue

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
