# ---- Build stage: compile the app with a full JDK ----
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy only the build files first, so Docker caches the dependency download.
# Changing code under src/ then doesn't re-download dependencies.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -ntp dependency:go-offline

# Now copy the source and build the jar.
# Tests are skipped here: Testcontainers needs Docker, which isn't available inside a Docker build.
COPY src src
RUN ./mvnw -B -ntp package -DskipTests

# ---- Run stage: only a JRE and the built jar ----
FROM eclipse-temurin:25-jre
WORKDIR /app

# Don't run the app as root.
RUN groupadd --system app && useradd --system --gid app app
USER app

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
