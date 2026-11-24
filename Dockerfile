FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY financeiro/financeiro/pom.xml ./pom.xml
COPY financeiro/financeiro/src ./src
RUN mvn -B -DskipTests package \
    && cp target/*.jar /build/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/app.jar ./app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=55.0"
CMD ["sh", "-c", "exec java -jar /app/app.jar --server.port=${PORT:-10000} --server.address=0.0.0.0"]
