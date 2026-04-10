# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
ARG MAVEN_MIRROR=https://maven.aliyun.com/repository/public

RUN mkdir -p /root/.m2 && \
    echo '<settings><mirrors><mirror><id>custom</id><mirrorOf>*</mirrorOf><url>'${MAVEN_MIRROR}'</url></mirror></mirrors></settings>' \
    > /root/.m2/settings.xml

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true
RUN test -f /app/target/story-weaver-backend.jar

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/story-weaver-backend.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
