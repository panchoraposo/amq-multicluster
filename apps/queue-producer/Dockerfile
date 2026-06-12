FROM registry.access.redhat.com/ubi9/openjdk-21:1.22 AS build
USER 0
RUN microdnf install -y maven && microdnf clean all
WORKDIR /workspace

COPY pom.xml pom.xml
COPY apps/queue-producer/pom.xml apps/queue-producer/pom.xml
COPY apps/queue-producer/src apps/queue-producer/src

RUN mvn -q -DskipTests -f apps/queue-producer/pom.xml package

FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.22
WORKDIR /work
COPY --from=build /workspace/apps/queue-producer/target/*-runner.jar /work/app.jar
CMD ["java","-jar","/work/app.jar"]

