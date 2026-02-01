FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# it download both the dependency and maven plugin (mvn dependency:resolve will only download the dependencies but will be forced to connect 
# to the intenet to fetch maven plugins which defeats the purpose of cache)
RUN mvn dependency:go-offline 

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT [ "java", "-jar", "app.jar" ]