ARG VERSION=11

FROM openjdk:${VERSION}-jdk as BUILD

COPY . /src
WORKDIR /src
RUN ./gradlew --no-daemon shadowJar

FROM openjdk:${VERSION}-jre

EXPOSE 8080
COPY --from=BUILD /src/build/libs/MERV-all.jar /bin/runner/run.jar
WORKDIR /bin/runner

CMD ["java","-jar","run.jar"]