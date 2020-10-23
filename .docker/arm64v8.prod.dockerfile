FROM arm64v8/openjdk:8u201-jdk-alpine

RUN mkdir /app

WORKDIR /app

RUN apk add git && \
    git clone --single-branch --branch v0.2.1 https://github.com/ivan-brko/GamayunArtifacts.git && \
    mv GamayunArtifacts/* . && \
    rm -rf GamayunArtifacts && \
    apk del git

ENTRYPOINT ["java","-jar","/app/Gamayun-0.2.1-all.jar"]
