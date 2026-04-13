# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
ARG MAVEN_MIRROR=https://maven.aliyun.com/repository/public

RUN mkdir -p /root/.m2 && \
    echo '<settings><mirrors><mirror><id>custom</id><mirrorOf>*</mirrorOf><url>'${MAVEN_MIRROR}'</url></mirror></mirrors></settings>' \
    > /root/.m2/settings.xml

WORKDIR /app
COPY pom.xml ./pom.xml
COPY backend/modules/story-domain/pom.xml ./backend/modules/story-domain/pom.xml
COPY backend/modules/story-storyunit/pom.xml ./backend/modules/story-storyunit/pom.xml
COPY backend/modules/story-generation/pom.xml ./backend/modules/story-generation/pom.xml
COPY backend/modules/story-provider/pom.xml ./backend/modules/story-provider/pom.xml
COPY backend/modules/story-web/pom.xml ./backend/modules/story-web/pom.xml
COPY backend/modules/story-infra/pom.xml ./backend/modules/story-infra/pom.xml
COPY backend/pom.xml ./backend/pom.xml
RUN mvn -pl backend -am dependency:go-offline -B
COPY backend/modules/story-domain/src ./backend/modules/story-domain/src
COPY backend/modules/story-storyunit/src ./backend/modules/story-storyunit/src
COPY backend/modules/story-generation/src ./backend/modules/story-generation/src
COPY backend/modules/story-provider/src ./backend/modules/story-provider/src
COPY backend/modules/story-web/src ./backend/modules/story-web/src
COPY backend/modules/story-infra/src ./backend/modules/story-infra/src
COPY backend/src ./backend/src
RUN mvn -pl backend -am clean package -Dmaven.test.skip=true
RUN test -f /app/backend/target/story-weaver-backend.jar

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/backend/target/story-weaver-backend.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
