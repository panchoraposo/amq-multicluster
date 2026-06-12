FROM registry.access.redhat.com/ubi9/openjdk-21:1.22 AS build
USER 0
RUN microdnf install -y maven && microdnf clean all
WORKDIR /workspace

COPY pom.xml pom.xml
COPY apps/device-simulator/pom.xml apps/device-simulator/pom.xml
COPY apps/device-simulator/src apps/device-simulator/src

RUN mvn -q -DskipTests -pl apps/device-simulator package

FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.22
WORKDIR /work
COPY --from=build /workspace/apps/device-simulator/target/*-runner.jar /work/app.jar
EXPOSE 8080
CMD ["java","-jar","/work/app.jar"]

