# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
ARG MAVEN_MIRROR=https://maven.aliyun.com/repository/public

RUN mkdir -p /root/.m2 && \
    echo '<settings><mirrors><mirror><id>custom</id><mirrorOf>*</mirrorOf><url>'${MAVEN_MIRROR}'</url></mirror></mirrors></settings>' \
    > /root/.m2/settings.xml

WORKDIR /app
COPY pom.xml ./pom.xml
COPY story-domain/pom.xml ./story-domain/pom.xml
COPY story-storyunit/pom.xml ./story-storyunit/pom.xml
COPY story-generation/pom.xml ./story-generation/pom.xml
COPY story-provider/pom.xml ./story-provider/pom.xml
COPY story-web/pom.xml ./story-web/pom.xml
COPY story-infra/pom.xml ./story-infra/pom.xml
COPY backend/pom.xml ./backend/pom.xml
RUN mvn -pl backend -am dependency:go-offline -B
COPY story-domain/src ./story-domain/src
COPY story-storyunit/src ./story-storyunit/src
COPY story-generation/src ./story-generation/src
COPY story-provider/src ./story-provider/src
COPY story-web/src ./story-web/src
COPY story-infra/src ./story-infra/src
COPY backend/src ./backend/src
RUN mvn -pl backend -am clean package -Dmaven.test.skip=true
RUN test -f /app/backend/target/story-weaver-backend.jar

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/target/story-weaver-backend.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
