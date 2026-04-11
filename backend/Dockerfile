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
COPY backend/pom.xml ./backend/pom.xml
RUN mvn -pl backend -am dependency:go-offline -B
COPY story-domain/src ./story-domain/src
COPY story-storyunit/src ./story-storyunit/src
COPY backend/src ./backend/src
RUN mvn -pl backend -am clean package -Dmaven.test.skip=true
RUN test -f /app/backend/target/story-weaver-backend.jar

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/target/story-weaver-backend.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
