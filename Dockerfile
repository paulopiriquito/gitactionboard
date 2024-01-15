FROM node:20.10-alpine as build-frontend
WORKDIR /app

COPY frontend/package.json frontend/yarn.lock ./
RUN yarn

COPY ./frontend .
RUN yarn build

FROM gradle:8.5.0-jdk21 as build-backend
WORKDIR /app

COPY ./backend/src ./src
COPY ./backend/gradle ./gradle
COPY ./backend/build.gradle ./build.gradle
COPY ./backend/settings.gradle ./settings.gradle
COPY ./backend/lombok.config ./lombok.config

COPY --from=build-frontend /app/dist ./src/main/resources/static

RUN gradle clean bootJar --no-daemon --stacktrace

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk upgrade libssl3 libcrypto3
COPY --from=build-backend /app/build/libs/gitactionboard.jar ./

EXPOSE 8080
CMD ["java", "-jar", "gitactionboard.jar"]