FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY core/pom.xml core/pom.xml
COPY api/pom.xml api/pom.xml
COPY core/src core/src
COPY api/src api/src
RUN mvn -B -pl api -am -Dmaven.test.skip=true -Djacoco.skip=true clean package

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
ENV SPRING_PROFILE=postgres
ENV PORT=8082
COPY --from=build /app/api/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
