FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/quarkus-app/ /app/

EXPOSE 8080
USER 1001
ENTRYPOINT ["java", "-Dquarkus.http.host=0.0.0.0", "-jar", "quarkus-run.jar"]
