FROM eclipse-temurin:17-ubi10
WORKDIR /app
COPY target/plan-external-0.0.1-SNAPSHOT.jar /app/app.jar
ENV SPRING_CONFIG_LOCATION=file:///samples/application.yaml
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]


