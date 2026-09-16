# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw

COPY src src
RUN ./mvnw -q -B -DskipTests package \
	&& cp target/ats-system-*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd --system --create-home --uid 10001 appsuser
COPY --from=build /workspace/app.jar /app/app.jar
USER appsuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
