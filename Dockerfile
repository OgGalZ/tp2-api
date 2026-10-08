# ---------- Étape 1 : build Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Les dépendances sont téléchargées dans une couche séparée :
# tant que le pom.xml ne change pas, elles restent en cache entre deux builds.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# ---------- Étape 2 : image d'exécution légère (JRE seul) ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Exécution avec un utilisateur non-root (bonne pratique de sécurité)
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /app/target/tp2-api-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
