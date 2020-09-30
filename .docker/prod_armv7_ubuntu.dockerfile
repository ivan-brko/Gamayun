
# FROM gradle:6.6.0-jdk8 AS build
FROM arm32v7/ubuntu:18.04
RUN mkdir /app && \
    apt-get update && \
    apt-get install openjdk-8-jdk && \
    apt-get install gradle

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon

# for some reason apt-get install doesn't work on 20.04
FROM arm32v7/ubuntu:18.04

RUN mkdir /app && \
    apt-get update && \
    apt-get install openjdk-8-jre

COPY --from=build /home/gradle/src/build/libs/ /app/

ENTRYPOINT ["java","-jar","/app/Gamayun-0.2-all.jar"]